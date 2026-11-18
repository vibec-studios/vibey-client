package dev.henny.vibeyclient.module.devlink;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * Minimal RFC 6455 WebSocket server without dependencies (no Minecraft classes either, so it can be tested on its
 * own). Binds to the loopback interface only, one acceptor thread plus one daemon thread per connection.
 * Supports text/binary messages with fragmentation, ping/pong, close codes and a message size limit; no extensions,
 * no subprotocols. Callbacks run on the connection's thread; {@link Connection#sendText} is thread safe.
 */
public final class WebSocketServer {
    /** Close codes (RFC 6455 §7.4.1 plus our own). */
    public static final int NORMAL = 1000, GOING_AWAY = 1001, PROTOCOL_ERROR = 1002, INVALID_DATA = 1007,
            TOO_BIG = 1009, INTERNAL_ERROR = 1011, TRY_AGAIN_LATER = 1013, UNAUTHORIZED = 4001;

    private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final int MAX_HEADER_BYTES = 16 * 1024;
    private static final int HANDSHAKE_TIMEOUT_MS = 5000;
    private static final int CLOSE_WAIT_MS = 2000;

    /** What a server does with its connections. Called on the connection thread. */
    public interface Handler {
        /** After the upgrade (and the remote address check). */
        default void onOpen(Connection c) {}

        void onMessage(Connection c, String text);

        /** Once per connection, after the socket is gone. {@code code} is what we sent or received (1006 = dropped). */
        default void onClose(Connection c, int code, String reason) {}

        /** The connection said nothing for the idle timeout and did not answer our ping. Default: close it. */
        default void onIdle(Connection c) {
            c.close(GOING_AWAY, "idle");
        }
    }

    private final int port;
    private final int maxMessageBytes;
    private final int maxConnections;
    private final Handler handler;
    private final Predicate<InetAddress> allowRemote;
    private final List<Connection> connections = new CopyOnWriteArrayList<>();
    private final AtomicInteger ids = new AtomicInteger();
    private final ScheduledExecutorService timer;
    private volatile ServerSocket server;
    private volatile boolean running;
    private boolean started;

    /**
     * @param allowRemote decides which peer addresses may talk to us; others get the upgrade and then close 4001.
     *                    Production: {@link InetAddress#isLoopbackAddress()}.
     */
    public WebSocketServer(int port, int maxMessageBytes, int maxConnections, Predicate<InetAddress> allowRemote, Handler handler) {
        this.port = port;
        this.maxMessageBytes = maxMessageBytes;
        this.maxConnections = maxConnections;
        this.allowRemote = allowRemote;
        this.handler = handler;
        this.timer = Executors.newSingleThreadScheduledExecutor(r -> daemon(r, "VibeyClient-DevLink-Timer"));
    }

    /** Binds 127.0.0.1:port (throws e.g. {@link java.net.BindException} when the port is in use) and starts accepting. */
    public synchronized void start() throws IOException {
        if (started) throw new IllegalStateException("A WebSocketServer can only be started once");
        started = true;
        ServerSocket s = new ServerSocket();
        try {
            s.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 16);
        } catch (IOException e) {
            closeQuietly(s);
            timer.shutdownNow();
            throw e;
        }
        server = s;
        running = true;
        daemon(this::acceptLoop, "VibeyClient-DevLink-Accept").start();
    }

    /** Stops accepting and closes every connection with {@code code}. Idempotent. */
    public void stop(int code, String reason) {
        synchronized (this) {
            if (!running) return;
            running = false;
        }
        closeQuietly(server);
        for (Connection c : connections) c.close(code, reason);
        // The timer still force-closes sockets whose peer does not answer the close frame; then it ends.
        timer.schedule(timer::shutdown, CLOSE_WAIT_MS + 500L, TimeUnit.MILLISECONDS);
    }

    public boolean isRunning() {
        return running;
    }

    /** The bound port (useful with port 0 in tests). */
    public int localPort() {
        ServerSocket s = server;
        return s == null ? -1 : s.getLocalPort();
    }

    /** Open connections (including ones that are not authenticated yet). */
    public List<Connection> connections() {
        return List.copyOf(connections);
    }

    private void acceptLoop() {
        ServerSocket s = server;
        while (running) {
            Socket socket;
            try {
                socket = s.accept();
            } catch (IOException e) {
                if (running) {
                    // Transient accept error (e.g. too many open files): do not spin.
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ie) {
                        return;
                    }
                    continue;
                }
                return;
            }
            Connection c = new Connection(socket, ids.incrementAndGet());
            daemon(c::run, "VibeyClient-DevLink-" + c.id).start();
        }
    }

    // ================================================================================================ connection

    public final class Connection {
        private final Socket socket;
        private final int id;
        private final Object writeLock = new Object();
        private volatile OutputStream out;
        private volatile boolean closeSent;
        private volatile boolean closed;
        private volatile int idleTimeoutMs = 30_000;
        private volatile int closeCode = 1006;
        private volatile String closeReason = "";
        /** Free for the handler (e.g. "authenticated"). */
        public volatile Object attachment;

        Connection(Socket socket, int id) {
            this.socket = socket;
            this.id = id;
        }

        public int id() {
            return id;
        }

        public InetAddress remoteAddress() {
            return socket.getInetAddress();
        }

        public boolean isOpen() {
            return !closed && !closeSent;
        }

        /**
         * Read timeout. When nothing arrives for that long we send a ping; when the next timeout passes as well,
         * {@link Handler#onIdle} is called. Takes effect with the next read.
         */
        public void setIdleTimeoutMs(int ms) {
            idleTimeoutMs = ms;
        }

        /** Sends a text message (one frame). False if the connection is closing or the write failed. */
        public boolean sendText(String text) {
            return send(0x1, text.getBytes(StandardCharsets.UTF_8));
        }

        /** Sends a close frame and ends the connection (after the peer answers, at most ~2 s later). */
        public void close(int code, String reason) {
            byte[] r = reason == null ? new byte[0] : reason.getBytes(StandardCharsets.UTF_8);
            if (r.length > 123) r = java.util.Arrays.copyOf(r, 123); // control frame payload ≤ 125
            byte[] payload = new byte[2 + r.length];
            payload[0] = (byte) (code >> 8);
            payload[1] = (byte) code;
            System.arraycopy(r, 0, payload, 2, r.length);
            synchronized (writeLock) {
                if (closeSent || closed) return;
                closeCode = code;
                closeReason = reason == null ? "" : reason;
                writeFrame(0x8, payload);
                closeSent = true;
            }
            try {
                timer.schedule(this::terminate, CLOSE_WAIT_MS, TimeUnit.MILLISECONDS);
            } catch (RuntimeException e) {
                terminate(); // timer already shut down
            }
        }

        /** Runs {@code task} on the server's timer thread after {@code ms} (null if the server is gone). */
        public ScheduledFuture<?> schedule(long ms, Runnable task) {
            try {
                return timer.schedule(task, ms, TimeUnit.MILLISECONDS);
            } catch (RuntimeException e) {
                return null;
            }
        }

        /** Drops the socket immediately (no close frame). */
        public void terminate() {
            closeQuietly(socket);
        }

        private boolean send(int opcode, byte[] payload) {
            synchronized (writeLock) {
                if (closeSent || closed || out == null) return false;
                return writeFrame(opcode, payload);
            }
        }

        /** Caller holds writeLock. */
        private boolean writeFrame(int opcode, byte[] payload) {
            OutputStream o = out;
            if (o == null) return false;
            try {
                int len = payload.length;
                byte[] head;
                if (len < 126) {
                    head = new byte[] {(byte) (0x80 | opcode), (byte) len};
                } else if (len <= 0xFFFF) {
                    head = new byte[] {(byte) (0x80 | opcode), 126, (byte) (len >> 8), (byte) len};
                } else {
                    head = new byte[10];
                    head[0] = (byte) (0x80 | opcode);
                    head[1] = 127;
                    for (int i = 0; i < 8; i++) head[2 + i] = (byte) ((long) len >>> (56 - 8 * i));
                }
                o.write(head);
                o.write(payload);
                o.flush();
                return true;
            } catch (IOException e) {
                terminate();
                return false;
            }
        }

        private void run() {
            boolean registered = false;
            try {
                if (connections.size() >= maxConnections) {
                    // Answer the handshake so the client sees a proper close code instead of a reset.
                    if (handshake()) close(TRY_AGAIN_LATER, "too many connections");
                    drainUntilClosed();
                    return;
                }
                connections.add(this);
                registered = true;
                if (!handshake()) return;
                if (!allowRemote.test(socket.getInetAddress())) {
                    close(UNAUTHORIZED, "loopback only");
                    drainUntilClosed();
                    return;
                }
                handler.onOpen(this);
                readLoop();
            } catch (Throwable t) {
                if (!closeSent && !closed) close(INTERNAL_ERROR, "internal error");
            } finally {
                closed = true;
                closeQuietly(socket);
                if (registered) {
                    connections.remove(this);
                    try {
                        handler.onClose(this, closeCode, closeReason);
                    } catch (RuntimeException ignored) {
                        // handler problems must not leak the thread
                    }
                }
            }
        }

        // ------------------------------------------------------------------------------------------- handshake

        private InputStream in;

        private boolean handshake() throws IOException {
            socket.setSoTimeout(HANDSHAKE_TIMEOUT_MS);
            socket.setTcpNoDelay(true);
            in = new BufferedInputStream(socket.getInputStream(), 16 * 1024);
            OutputStream rawOut = socket.getOutputStream();
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(HANDSHAKE_TIMEOUT_MS * 2L);
            String head;
            try {
                head = readHead(deadline);
            } catch (SocketTimeoutException | EOFException e) {
                return false;
            }
            if (head == null) {
                httpError(rawOut, "431 Request Header Fields Too Large", null);
                return false;
            }
            String[] lines = head.split("\r\n");
            String[] request = lines[0].split(" ");
            Map<String, String> headers = new HashMap<>();
            for (int i = 1; i < lines.length; i++) {
                int colon = lines[i].indexOf(':');
                if (colon <= 0) continue;
                headers.merge(lines[i].substring(0, colon).trim().toLowerCase(Locale.ROOT), lines[i].substring(colon + 1).trim(),
                        (a, b) -> a + ", " + b);
            }
            if (request.length < 3 || !request[0].equals("GET") || !request[2].startsWith("HTTP/1.1")) {
                httpError(rawOut, "400 Bad Request", null);
                return false;
            }
            String upgrade = headers.getOrDefault("upgrade", "").toLowerCase(Locale.ROOT);
            String connection = headers.getOrDefault("connection", "").toLowerCase(Locale.ROOT);
            String key = headers.get("sec-websocket-key");
            if (!upgrade.contains("websocket") || !connection.contains("upgrade") || key == null) {
                httpError(rawOut, "426 Upgrade Required", "Upgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Version: 13\r\n");
                return false;
            }
            if (!"13".equals(headers.get("sec-websocket-version"))) {
                httpError(rawOut, "426 Upgrade Required", "Sec-WebSocket-Version: 13\r\n");
                return false;
            }
            byte[] keyBytes;
            try {
                keyBytes = Base64.getDecoder().decode(key);
            } catch (IllegalArgumentException e) {
                keyBytes = null;
            }
            if (keyBytes == null || keyBytes.length != 16) {
                httpError(rawOut, "400 Bad Request", null);
                return false;
            }
            String response = "HTTP/1.1 101 Switching Protocols\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Sec-WebSocket-Accept: " + acceptKey(key) + "\r\n\r\n";
            synchronized (writeLock) {
                rawOut.write(response.getBytes(StandardCharsets.US_ASCII));
                rawOut.flush();
                out = rawOut;
            }
            return true;
        }

        /** Reads up to the blank line; null if the head is too big. */
        private String readHead(long deadline) throws IOException {
            ByteArrayOutputStream buf = new ByteArrayOutputStream(512);
            int state = 0; // matched chars of \r\n\r\n
            while (state < 4) {
                if (System.nanoTime() > deadline) throw new SocketTimeoutException("handshake deadline");
                int b = in.read();
                if (b < 0) throw new EOFException();
                buf.write(b);
                if (buf.size() > MAX_HEADER_BYTES) return null;
                if (b == '\r') state = state == 2 ? 3 : 1;
                else if (b == '\n') state = state == 1 ? 2 : state == 3 ? 4 : 0;
                else state = 0;
            }
            return buf.toString(StandardCharsets.ISO_8859_1);
        }

        private void httpError(OutputStream o, String status, String extraHeaders) {
            try {
                String body = status + "\n";
                o.write(("HTTP/1.1 " + status + "\r\n" + (extraHeaders == null ? "" : extraHeaders)
                        + "Content-Type: text/plain\r\nContent-Length: " + body.length() + "\r\nConnection: close\r\n\r\n" + body)
                        .getBytes(StandardCharsets.US_ASCII));
                o.flush();
            } catch (IOException ignored) {
                // the peer is gone anyway
            }
        }

        // ------------------------------------------------------------------------------------------- frames

        private void readLoop() throws IOException {
            ByteArrayOutputStream message = null;
            int messageOpcode = 0;
            boolean pingSent = false;
            while (!closed) {
                int b0;
                try {
                    socket.setSoTimeout(closeSent ? CLOSE_WAIT_MS : idleTimeoutMs);
                    b0 = in.read();
                } catch (SocketTimeoutException e) {
                    if (closeSent) return;
                    if (pingSent) {
                        handler.onIdle(this);
                        if (closeSent) continue;
                    } else {
                        send(0x9, new byte[0]);
                    }
                    pingSent = !pingSent;
                    continue;
                } catch (SocketException e) {
                    return;
                }
                if (b0 < 0) return;
                pingSent = false;
                // The rest of a frame header must arrive in time too.
                socket.setSoTimeout(HANDSHAKE_TIMEOUT_MS);
                int b1 = readByte();
                boolean fin = (b0 & 0x80) != 0;
                int rsv = b0 & 0x70;
                int opcode = b0 & 0x0F;
                boolean masked = (b1 & 0x80) != 0;
                long len = b1 & 0x7F;
                if (len == 126) {
                    len = (readByte() << 8) | readByte();
                } else if (len == 127) {
                    len = 0;
                    for (int i = 0; i < 8; i++) len = (len << 8) | readByte();
                    if (len < 0) {
                        fail(TOO_BIG, "message too big");
                        return;
                    }
                }
                if (rsv != 0 || !masked) {
                    fail(PROTOCOL_ERROR, rsv != 0 ? "no extensions negotiated" : "client frames must be masked");
                    return;
                }
                boolean control = (opcode & 0x8) != 0;
                if (control && (!fin || len > 125)) {
                    fail(PROTOCOL_ERROR, "bad control frame");
                    return;
                }
                if (!control) {
                    if (opcode != 0x0 && opcode != 0x1 && opcode != 0x2) {
                        fail(PROTOCOL_ERROR, "unknown opcode");
                        return;
                    }
                    long already = message == null ? 0 : message.size();
                    if (already + len > maxMessageBytes) {
                        fail(TOO_BIG, "message too big");
                        return;
                    }
                }
                byte[] mask = readFully(4);
                // Payloads are read in chunks so a lying length cannot make us allocate memory up front.
                socket.setSoTimeout(Math.max(idleTimeoutMs, HANDSHAKE_TIMEOUT_MS));
                byte[] payload = readPayload(len, mask);
                switch (opcode) {
                    case 0x8 -> {
                        onCloseFrame(payload);
                        return;
                    }
                    case 0x9 -> send(0xA, payload);
                    case 0xA -> { /* pong: just proves liveness */ }
                    case 0x0 -> {
                        if (message == null) {
                            fail(PROTOCOL_ERROR, "continuation without a message");
                            return;
                        }
                        message.write(payload);
                        if (fin) {
                            byte[] all = message.toByteArray();
                            message = null;
                            if (!deliver(messageOpcode, all)) return;
                        }
                    }
                    default -> {
                        if (message != null) {
                            fail(PROTOCOL_ERROR, "new message inside a fragmented one");
                            return;
                        }
                        if (fin) {
                            if (!deliver(opcode, payload)) return;
                        } else {
                            message = new ByteArrayOutputStream(Math.max(256, payload.length * 2));
                            message.write(payload);
                            messageOpcode = opcode;
                        }
                    }
                }
            }
        }

        /** Hands a complete message to the handler. False if the connection must end (bad UTF-8). */
        private boolean deliver(int opcode, byte[] data) {
            if (closeSent) return true; // closing: drop data until the peer's close frame
            String text;
            try {
                text = StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(data)).toString();
            } catch (CharacterCodingException e) {
                if (opcode == 0x1) {
                    fail(INVALID_DATA, "text is not UTF-8");
                    return false;
                }
                text = new String(data, StandardCharsets.UTF_8);
            }
            handler.onMessage(this, text);
            return true;
        }

        private void onCloseFrame(byte[] payload) {
            if (payload.length == 1) {
                fail(PROTOCOL_ERROR, "bad close frame");
                return;
            }
            int code = payload.length >= 2 ? ((payload[0] & 0xFF) << 8) | (payload[1] & 0xFF) : NORMAL;
            if (!closeSent) {
                // Echo the peer's close and record what it said.
                String reason = payload.length > 2 ? new String(payload, 2, payload.length - 2, StandardCharsets.UTF_8) : "";
                synchronized (writeLock) {
                    if (!closeSent) {
                        byte[] echo = payload.length >= 2 ? new byte[] {payload[0], payload[1]} : new byte[0];
                        writeFrame(0x8, echo);
                        closeSent = true;
                        closeCode = code;
                        closeReason = reason;
                    }
                }
            }
        }

        private void fail(int code, String reason) {
            close(code, reason);
            drainUntilClosed();
        }

        /** After our close frame: wait (bounded) for the peer's close so it receives the code instead of a reset. */
        private void drainUntilClosed() {
            try {
                socket.setSoTimeout(CLOSE_WAIT_MS);
                long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(CLOSE_WAIT_MS);
                byte[] sink = new byte[8192];
                InputStream i = in != null ? in : socket.getInputStream();
                while (System.nanoTime() < deadline && i.read(sink) >= 0) {
                    // discard
                }
            } catch (IOException ignored) {
                // timeout or reset: done
            }
        }

        private int readByte() throws IOException {
            int b = in.read();
            if (b < 0) throw new EOFException();
            return b;
        }

        private byte[] readFully(int n) throws IOException {
            byte[] b = new byte[n];
            int off = 0;
            while (off < n) {
                int r = in.read(b, off, n - off);
                if (r < 0) throw new EOFException();
                off += r;
            }
            return b;
        }

        private byte[] readPayload(long len, byte[] mask) throws IOException {
            ByteArrayOutputStream buf = new ByteArrayOutputStream((int) Math.min(len, 64 * 1024));
            byte[] chunk = new byte[(int) Math.min(Math.max(len, 1), 64 * 1024)];
            long left = len;
            long pos = 0;
            while (left > 0) {
                int r = in.read(chunk, 0, (int) Math.min(chunk.length, left));
                if (r < 0) throw new EOFException();
                for (int i = 0; i < r; i++) chunk[i] ^= mask[(int) ((pos + i) & 3)];
                buf.write(chunk, 0, r);
                pos += r;
                left -= r;
            }
            return buf.toByteArray();
        }
    }

    // ================================================================================================ helpers

    static String acceptKey(String key) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            return Base64.getEncoder().encodeToString(sha1.digest((key + GUID).getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Thread daemon(Runnable r, String name) {
        Thread t = new Thread(r, name);
        t.setDaemon(true);
        return t;
    }

    private static void closeQuietly(AutoCloseable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Exception ignored) {
            // nothing to do
        }
    }
}
