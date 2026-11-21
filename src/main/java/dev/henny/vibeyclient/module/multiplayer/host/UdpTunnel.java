package dev.henny.vibeyclient.module.multiplayer.host;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class UdpTunnel {
    public static final int CHANNEL_VOICE = 1;
    public static final int CHANNEL_BEDROCK = 2;

    static final byte[] MAGIC = {0x00, 0x56, 0x42, 0x55};
    static final byte HELLO = 0x01;
    static final byte DATA = 0x02;
    static final byte CLOSE = 0x03;
    static final byte ACK = (byte) 0x81;
    static final byte RELAY_DATA = (byte) 0x82;
    static final byte RELAY_CLOSE = (byte) 0x83;
    static final int MAX_PAYLOAD = 2048;

    private static final Logger LOG = LoggerFactory.getLogger("VibeyClient/multiplayer-udp");

    private final String relayHost;
    private final Map<Integer, Integer> localPorts = new ConcurrentHashMap<>();
    private final Map<Integer, Peer> peers = new HashMap<>();
    private final long helloMs;
    private final long fastHelloMs;
    private final long idleMs;

    private volatile byte[] token;
    private volatile InetSocketAddress relay;
    private volatile boolean helloNow = true;
    private volatile boolean closed;
    private volatile long ackedAt;
    private volatile int peerCount;
    private long firstHelloAt;
    private long nextHelloAt;
    private long nextSweepAt;
    private Selector selector;
    private DatagramChannel tunnel;
    private Thread thread;

    private static final class Peer {
        final int id;
        final int port;
        final DatagramChannel channel;
        long last;

        Peer(int id, int port, DatagramChannel channel) {
            this.id = id;
            this.port = port;
            this.channel = channel;
        }
    }

    public UdpTunnel(String relayHost, int tunnelPort, String token, Map<Integer, Integer> local) {
        this(relayHost, tunnelPort, token, local, 15_000, 2_000, 60_000);
    }

    UdpTunnel(String relayHost, int tunnelPort, String token, Map<Integer, Integer> local, long helloMs, long fastHelloMs, long idleMs) {
        this.relayHost = relayHost;
        this.relay = new InetSocketAddress(relayHost, tunnelPort);
        this.token = UdpReady.tokenBytes(token);
        this.localPorts.putAll(local);
        this.helloMs = helloMs;
        this.fastHelloMs = fastHelloMs;
        this.idleMs = idleMs;
    }

    public void start() throws IOException {
        selector = Selector.open();
        tunnel = DatagramChannel.open();
        tunnel.configureBlocking(false);
        tunnel.bind(new InetSocketAddress(0));
        tunnel.register(selector, SelectionKey.OP_READ, null);
        thread = new Thread(this::run, "VibeyClient-Mp-Udp");
        thread.setDaemon(true);
        thread.start();
    }

    public void update(String newToken, int tunnelPort) {
        token = UdpReady.tokenBytes(newToken);
        relay = new InetSocketAddress(relayHost, tunnelPort);
        ackedAt = 0;
        helloNow = true;
        Selector s = selector;
        if (s != null) s.wakeup();
    }

    public void setLocalPort(int channel, int port) {
        if (port > 0) localPorts.put(channel, port);
        else localPorts.remove(channel);
    }

    public boolean acked() {
        return ackedAt != 0;
    }

    public int peers() {
        return peerCount;
    }

    int localPort() throws IOException {
        return ((InetSocketAddress) tunnel.getLocalAddress()).getPort();
    }

    public void close() {
        closed = true;
        Selector s = selector;
        if (s != null) s.wakeup();
    }

    private void run() {
        ByteBuffer buf = ByteBuffer.allocate(MAX_PAYLOAD + 64);
        try {
            while (!closed) {
                long now = System.currentTimeMillis();
                timers(now);
                selector.select(Math.max(1, Math.min(nextHelloAt, nextSweepAt) - now));
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    if (!key.isValid() || !key.isReadable()) continue;
                    if (key.attachment() instanceof Peer p) fromLocal(p, buf);
                    else fromRelay(buf);
                }
            }
        } catch (IOException e) {
            if (!closed) LOG.info("UDP tunnel stopped: {}", e.toString());
        } finally {
            for (Peer p : peers.values()) quiet(p.channel);
            peers.clear();
            peerCount = 0;
            quiet(tunnel);
            try {
                selector.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void timers(long now) {
        if (helloNow || now >= nextHelloAt) {
            if (helloNow) firstHelloAt = now;
            helloNow = false;
            sendHello();
            boolean fast = ackedAt == 0 && now - firstHelloAt < 30_000;
            nextHelloAt = now + (fast ? fastHelloMs : helloMs);
        }
        if (now >= nextSweepAt) {
            Iterator<Peer> it = peers.values().iterator();
            while (it.hasNext()) {
                Peer p = it.next();
                if (now - p.last > idleMs) {
                    it.remove();
                    quiet(p.channel);
                    toRelay(CLOSE, p.id, null);
                }
            }
            peerCount = peers.size();
            nextSweepAt = now + Math.min(idleMs, 5_000);
        }
    }

    private void fromRelay(ByteBuffer buf) throws IOException {
        buf.clear();
        SocketAddress from = tunnel.receive(buf);
        if (!(from instanceof InetSocketAddress src)) return;
        InetSocketAddress r = relay;
        if (src.getPort() != r.getPort() || r.getAddress() == null || !r.getAddress().equals(src.getAddress())) return;
        buf.flip();
        if (buf.remaining() < 5) return;
        for (byte b : MAGIC) if (buf.get() != b) return;
        byte type = buf.get();
        if (type == ACK) {
            ackedAt = System.currentTimeMillis();
            return;
        }
        if (buf.remaining() < 4) return;
        int id = buf.getInt();
        if (type == RELAY_CLOSE) {
            Peer p = peers.remove(id);
            if (p != null) quiet(p.channel);
            peerCount = peers.size();
            return;
        }
        if (type != RELAY_DATA || buf.remaining() < 1) return;
        int channel = buf.get() & 0xFF;
        Integer port = localPorts.get(channel);
        if (port == null) return;
        Peer p = peers.get(id);
        if (p == null || p.port != port) {
            if (p != null) quiet(p.channel);
            DatagramChannel local = DatagramChannel.open();
            local.configureBlocking(false);
            local.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0));
            p = new Peer(id, port, local);
            local.register(selector, SelectionKey.OP_READ, p);
            peers.put(id, p);
            peerCount = peers.size();
        }
        p.last = System.currentTimeMillis();
        p.channel.send(buf, new InetSocketAddress(InetAddress.getLoopbackAddress(), port));
    }

    private void fromLocal(Peer p, ByteBuffer buf) throws IOException {
        buf.clear();
        SocketAddress from = p.channel.receive(buf);
        if (!(from instanceof InetSocketAddress src) || src.getPort() != p.port || !src.getAddress().isLoopbackAddress()) return;
        buf.flip();
        if (buf.remaining() > MAX_PAYLOAD) return;
        p.last = System.currentTimeMillis();
        toRelay(DATA, p.id, buf);
    }

    private void sendHello() {
        toRelay(HELLO, -1, null);
    }

    private void toRelay(byte type, int peer, ByteBuffer payload) {
        byte[] t = token;
        boolean withPeer = type != HELLO;
        ByteBuffer out = ByteBuffer.allocate(4 + 1 + 16 + (withPeer ? 4 : 0) + (payload == null ? 0 : payload.remaining()));
        out.put(MAGIC).put(type).put(t);
        if (withPeer) out.putInt(peer);
        if (payload != null) out.put(payload);
        out.flip();
        try {
            tunnel.send(out, relay);
        } catch (IOException e) {
            LOG.debug("UDP send failed: {}", e.toString());
        }
    }

    private static void quiet(DatagramChannel c) {
        try {
            if (c != null) c.close();
        } catch (IOException ignored) {
        }
    }
}
