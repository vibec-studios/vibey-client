package dev.henny.vibeyclient.module.multiplayer.host;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UdpTunnelTest {
    static final String TOKEN = "00112233445566778899aabbccddeeff";

    private DatagramSocket relay;
    private DatagramSocket svc;
    private UdpTunnel tunnel;

    @AfterEach
    void tearDown() {
        if (tunnel != null) tunnel.close();
        if (relay != null) relay.close();
        if (svc != null) svc.close();
    }

    private void start(long idleMs) throws Exception {
        InetAddress lo = InetAddress.getLoopbackAddress();
        relay = new DatagramSocket(new InetSocketAddress(lo, 0));
        relay.setSoTimeout(3000);
        svc = new DatagramSocket(new InetSocketAddress(lo, 0));
        svc.setSoTimeout(3000);
        tunnel = new UdpTunnel("127.0.0.1", relay.getLocalPort(), TOKEN, Map.of(UdpTunnel.CHANNEL_VOICE, svc.getLocalPort()),
                15_000, 200, idleMs);
        tunnel.start();
    }

    static byte[] relayPacket(byte type, int peer, int channel, byte[] payload) {
        ByteBuffer b = ByteBuffer.allocate(4 + 1 + 4 + 1 + payload.length);
        b.put(UdpTunnel.MAGIC).put(type).putInt(peer).put((byte) channel).put(payload);
        return b.array();
    }

    static DatagramPacket receive(DatagramSocket s) throws Exception {
        DatagramPacket p = new DatagramPacket(new byte[4096], 4096);
        s.receive(p);
        return p;
    }

    static byte[] data(DatagramPacket p) {
        return Arrays.copyOfRange(p.getData(), p.getOffset(), p.getOffset() + p.getLength());
    }

    @Test
    void helloAckDataAndClose() throws Exception {
        start(60_000);
        DatagramPacket hello = receive(relay);
        byte[] h = data(hello);
        assertEquals(21, h.length);
        assertArrayEquals(UdpTunnel.MAGIC, Arrays.copyOfRange(h, 0, 4));
        assertEquals(UdpTunnel.HELLO, h[4]);
        assertArrayEquals(UdpReady.tokenBytes(TOKEN), Arrays.copyOfRange(h, 5, 21));
        InetSocketAddress host = (InetSocketAddress) hello.getSocketAddress();

        byte[] ack = ByteBuffer.allocate(5).put(UdpTunnel.MAGIC).put(UdpTunnel.ACK).array();
        relay.send(new DatagramPacket(ack, ack.length, host));
        for (int i = 0; i < 50 && !tunnel.acked(); i++) Thread.sleep(20);
        assertTrue(tunnel.acked());

        byte[] voice = {(byte) 0xFF, 1, 2, 3};
        byte[] in = relayPacket(UdpTunnel.RELAY_DATA, 0xFEED0001, UdpTunnel.CHANNEL_VOICE, voice);
        relay.send(new DatagramPacket(in, in.length, host));

        DatagramPacket atSvc = receive(svc);
        assertArrayEquals(voice, data(atSvc));
        assertTrue(atSvc.getAddress().isLoopbackAddress());
        assertEquals(1, tunnel.peers());

        byte[] answer = "pong".getBytes(StandardCharsets.UTF_8);
        svc.send(new DatagramPacket(answer, answer.length, atSvc.getSocketAddress()));

        byte[] out = nextNonHello();
        assertEquals(UdpTunnel.DATA, out[4]);
        assertArrayEquals(UdpReady.tokenBytes(TOKEN), Arrays.copyOfRange(out, 5, 21));
        assertEquals(0xFEED0001, ByteBuffer.wrap(out, 21, 4).getInt());
        assertArrayEquals(answer, Arrays.copyOfRange(out, 25, out.length));

        byte[] close = ByteBuffer.allocate(9).put(UdpTunnel.MAGIC).put(UdpTunnel.RELAY_CLOSE).putInt(0xFEED0001).array();
        relay.send(new DatagramPacket(close, close.length, host));
        for (int i = 0; i < 50 && tunnel.peers() > 0; i++) Thread.sleep(20);
        assertEquals(0, tunnel.peers());
    }

    @Test
    void ignoresStrangersAndUnknownChannels() throws Exception {
        start(60_000);
        InetSocketAddress host = (InetSocketAddress) receive(relay).getSocketAddress();
        byte[] bedrock = relayPacket(UdpTunnel.RELAY_DATA, 5, UdpTunnel.CHANNEL_BEDROCK, new byte[]{1});
        relay.send(new DatagramPacket(bedrock, bedrock.length, host));
        try (DatagramSocket stranger = new DatagramSocket(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0))) {
            byte[] voice = relayPacket(UdpTunnel.RELAY_DATA, 6, UdpTunnel.CHANNEL_VOICE, new byte[]{2});
            stranger.send(new DatagramPacket(voice, voice.length, host));
        }
        svc.setSoTimeout(400);
        boolean got = true;
        try {
            receive(svc);
        } catch (SocketTimeoutException e) {
            got = false;
        }
        assertTrue(!got, "nothing may reach the local server");
        assertEquals(0, tunnel.peers());
    }

    @Test
    void idlePeersAreClosedAndReported() throws Exception {
        start(300);
        InetSocketAddress host = (InetSocketAddress) receive(relay).getSocketAddress();
        byte[] in = relayPacket(UdpTunnel.RELAY_DATA, 42, UdpTunnel.CHANNEL_VOICE, new byte[]{9});
        relay.send(new DatagramPacket(in, in.length, host));
        receive(svc);
        byte[] out = nextNonHello();
        assertEquals(UdpTunnel.CLOSE, out[4]);
        assertEquals(42, ByteBuffer.wrap(out, 21, 4).getInt());
        assertEquals(0, tunnel.peers());
    }

    @Test
    void updateSendsHelloWithNewToken() throws Exception {
        start(60_000);
        receive(relay);
        String next = "ffeeddccbbaa99887766554433221100";
        tunnel.update(next, relay.getLocalPort());
        byte[] h;
        do {
            h = data(receive(relay));
        } while (!Arrays.equals(UdpReady.tokenBytes(next), Arrays.copyOfRange(h, 5, 21)));
        assertEquals(UdpTunnel.HELLO, h[4]);
    }

    private byte[] nextNonHello() throws Exception {
        while (true) {
            byte[] b = data(receive(relay));
            if (b[4] != UdpTunnel.HELLO) return b;
        }
    }
}
