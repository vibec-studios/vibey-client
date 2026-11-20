package dev.henny.vibeyclient.gametest;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

final class FakeStatusServer implements AutoCloseable {
    private final ServerSocket socket;
    private final String json;
    private final AtomicInteger pings = new AtomicInteger();

    private FakeStatusServer(String json) throws IOException {
        this.socket = new ServerSocket(0, 16, InetAddress.getLoopbackAddress());
        this.json = json;
        Thread t = new Thread(this::accept, "fake-status-server");
        t.setDaemon(true);
        t.start();
    }

    static FakeStatusServer start(String json) {
        try {
            return new FakeStatusServer(json);
        } catch (IOException e) {
            throw new AssertionError("fake status server", e);
        }
    }

    int port() {
        return socket.getLocalPort();
    }

    int pings() {
        return pings.get();
    }

    private void accept() {
        while (!socket.isClosed()) {
            try {
                Socket c = socket.accept();
                Thread t = new Thread(() -> serve(c), "fake-status-conn");
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                return;
            }
        }
    }

    private void serve(Socket c) {
        try (c) {
            c.setSoTimeout(5000);
            DataInputStream in = new DataInputStream(c.getInputStream());
            OutputStream out = c.getOutputStream();
            readPacket(in);
            readPacket(in);
            ByteArrayOutputStream status = new ByteArrayOutputStream();
            writeVarInt(status, 0);
            writeString(status, json);
            send(out, status.toByteArray());
            byte[] ping = readPacket(in);
            send(out, ping);
            pings.incrementAndGet();
        } catch (IOException ignored) {
        }
    }

    private static byte[] readPacket(DataInputStream in) throws IOException {
        int len = readVarInt(in);
        byte[] data = new byte[len];
        in.readFully(data);
        return data;
    }

    private static void send(OutputStream out, byte[] payload) throws IOException {
        ByteArrayOutputStream frame = new ByteArrayOutputStream();
        writeVarInt(frame, payload.length);
        frame.write(payload);
        out.write(frame.toByteArray());
        out.flush();
    }

    private static int readVarInt(InputStream in) throws IOException {
        int value = 0, shift = 0, b;
        do {
            b = in.read();
            if (b < 0) throw new IOException("eof");
            value |= (b & 0x7F) << shift;
            shift += 7;
        } while ((b & 0x80) != 0 && shift < 35);
        return value;
    }

    private static void writeVarInt(OutputStream out, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            out.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.write(value);
    }

    private static void writeString(ByteArrayOutputStream out, String s) throws IOException {
        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, b.length);
        new DataOutputStream(out).write(b);
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
