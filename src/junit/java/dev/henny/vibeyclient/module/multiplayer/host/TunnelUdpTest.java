package dev.henny.vibeyclient.module.multiplayer.host;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TunnelUdpTest {
    private static final String TOKEN = UdpTunnelTest.TOKEN;

    @Test
    void requestsVoiceAndForwardsThroughTheTunnel() throws Exception {
        InetAddress lo = InetAddress.getLoopbackAddress();
        List<Tunnel.UdpState> states = new CopyOnWriteArrayList<>();
        List<UdpReady> readies = new CopyOnWriteArrayList<>();
        try (ServerSocket control = new ServerSocket(0, 5, lo);
             DatagramSocket udpRelay = new DatagramSocket(new InetSocketAddress(lo, 0));
             DatagramSocket svc = new DatagramSocket(new InetSocketAddress(lo, 0))) {
            udpRelay.setSoTimeout(4000);
            svc.setSoTimeout(4000);
            control.setSoTimeout(4000);
            Tunnel tunnel = new Tunnel("127.0.0.1:" + control.getLocalPort(), 25565, "Host", UUID.randomUUID(), "1.21.11",
                    id -> null, () -> "{\"t\":\"access\"}", new Tunnel.Listener() {
                        @Override
                        public void onState(Tunnel.State state, String address, String error) {
                        }

                        @Override
                        public void onUdp(Tunnel.UdpState state, UdpReady ready) {
                            states.add(state);
                            if (ready != null) readies.add(ready);
                        }
                    }, () -> Map.of(UdpTunnel.CHANNEL_VOICE, svc.getLocalPort()));
            tunnel.start();
            try (Socket s = control.accept()) {
                s.setSoTimeout(4000);
                InputStream raw = s.getInputStream();
                byte[] magic = raw.readNBytes(4);
                assertArrayEquals(RelayHello.magic(), magic);
                BufferedReader in = new BufferedReader(new InputStreamReader(raw, StandardCharsets.UTF_8));
                OutputStream out = s.getOutputStream();
                assertEquals("hello", type(in.readLine()));
                assertEquals("access", type(in.readLine()));
                JsonObject udp = JsonParser.parseString(in.readLine()).getAsJsonObject();
                assertEquals("udp", udp.get("t").getAsString());
                assertEquals("voice", udp.getAsJsonArray("channels").get(0).getAsString());
                assertEquals(1, udp.getAsJsonArray("channels").size());

                send(out, "{\"t\":\"ready\",\"address\":\"mossy-otter-27.axolite.cc\",\"features\":[\"udp\"]}");
                send(out, "{\"t\":\"udp_ready\",\"token\":\"" + TOKEN + "\",\"tunnel\":{\"port\":" + udpRelay.getLocalPort()
                        + "},\"channels\":{\"voice\":{\"port\":24454,\"address\":\"mossy-otter-27.axolite.cc:24454\",\"voiceHost\":\"24454\"}},"
                        + "\"denied\":{\"bedrock\":{\"code\":\"disabled\",\"msg\":\"no\"}}}");

                DatagramPacket hello = UdpTunnelTest.receive(udpRelay);
                byte[] h = UdpTunnelTest.data(hello);
                assertEquals(UdpTunnel.HELLO, h[4]);
                assertArrayEquals(UdpReady.tokenBytes(TOKEN), Arrays.copyOfRange(h, 5, 21));
                send(out, "{\"t\":\"udp_ok\"}");
                waitFor(() -> tunnel.udpState() == Tunnel.UdpState.OK);
                assertEquals(Tunnel.UdpState.OK, tunnel.udpState());
                assertEquals("24454", readies.get(readies.size() - 1).voiceHost());
                assertEquals("disabled", readies.get(readies.size() - 1).denied().get("bedrock"));

                byte[] voice = {(byte) 0xFF, 7};
                byte[] pkt = UdpTunnelTest.relayPacket(UdpTunnel.RELAY_DATA, 3, UdpTunnel.CHANNEL_VOICE, voice);
                udpRelay.send(new DatagramPacket(pkt, pkt.length, hello.getSocketAddress()));
                assertArrayEquals(voice, UdpTunnelTest.data(UdpTunnelTest.receive(svc)));

                send(out, "{\"t\":\"udp_ready\",\"token\":\"\",\"channels\":{},\"denied\":{\"voice\":{\"code\":\"disabled\"}}}");
                waitFor(() -> tunnel.udpState() == Tunnel.UdpState.DENIED);
                assertEquals(Tunnel.UdpState.DENIED, tunnel.udpState());
            } finally {
                tunnel.close();
            }
        }
        assertTrue(states.contains(Tunnel.UdpState.WAITING));
        assertTrue(states.contains(Tunnel.UdpState.CONNECTING));
    }

    @Test
    void oldRelayWithoutUdpFeature() throws Exception {
        InetAddress lo = InetAddress.getLoopbackAddress();
        try (ServerSocket control = new ServerSocket(0, 5, lo)) {
            control.setSoTimeout(4000);
            Tunnel tunnel = new Tunnel("127.0.0.1:" + control.getLocalPort(), 25565, "Host", UUID.randomUUID(), "1.21.11",
                    id -> null, () -> "{\"t\":\"access\"}", (state, address, error) -> {}, () -> Map.of(UdpTunnel.CHANNEL_VOICE, 24454));
            tunnel.start();
            try (Socket s = control.accept()) {
                s.setSoTimeout(4000);
                s.getInputStream().readNBytes(4);
                send(s.getOutputStream(), "{\"t\":\"ready\",\"address\":\"a.axolite.cc\"}");
                waitFor(() -> tunnel.udpState() == Tunnel.UdpState.UNAVAILABLE);
                assertEquals(Tunnel.UdpState.UNAVAILABLE, tunnel.udpState());
            } finally {
                tunnel.close();
            }
        }
    }

    @Test
    void noUdpRequestWithoutChannels() {
        JsonObject req = Tunnel.udpRequest(Map.of(UdpTunnel.CHANNEL_VOICE, 1, UdpTunnel.CHANNEL_BEDROCK, 2));
        assertEquals("[\"voice\",\"bedrock\"]", req.getAsJsonArray("channels").toString());
    }

    @Test
    void parsesUdpReady() {
        JsonObject msg = JsonParser.parseString("{\"t\":\"udp_ready\",\"token\":\"" + TOKEN + "\",\"tunnel\":{\"port\":24454},"
                + "\"channels\":{\"bedrock\":{\"port\":19207,\"host\":\"a.axolite.cc\"}}}").getAsJsonObject();
        UdpReady r = UdpReady.parse(msg);
        assertTrue(r.open());
        assertTrue(!r.voice());
        assertEquals(24454, r.tunnelPort());
        assertEquals(19207, r.bedrockPort());
        assertEquals("a.axolite.cc", r.bedrockHost());
        assertTrue(!UdpReady.parse(JsonParser.parseString("{\"token\":\"\"}").getAsJsonObject()).open());
        ByteBuffer t = ByteBuffer.wrap(UdpReady.tokenBytes(TOKEN));
        assertEquals(0x00112233, t.getInt());
    }

    private static String type(String line) {
        return JsonParser.parseString(line).getAsJsonObject().get("t").getAsString();
    }

    private static void send(OutputStream out, String line) throws Exception {
        out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private static void waitFor(java.util.function.BooleanSupplier ok) throws InterruptedException {
        for (int i = 0; i < 300 && !ok.getAsBoolean(); i++) Thread.sleep(30);
    }
}
