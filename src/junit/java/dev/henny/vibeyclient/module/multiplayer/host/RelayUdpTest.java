package dev.henny.vibeyclient.module.multiplayer.host;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class RelayUdpTest {
    @Test
    void realRelayAcceptsTheUdpTunnel() throws Exception {
        String target = System.getenv("VIBEY_TEST_RELAY_UDP");
        assumeTrue(target != null && !target.isBlank(), "VIBEY_TEST_RELAY_UDP not set (relay 1.5+ with ALLOW_OFFLINE=1)");
        Tunnel tunnel = new Tunnel(target, 25565, "UdpTest", UUID.randomUUID(), "1.21.11", id -> null,
                () -> "{\"t\":\"access\"}", (state, address, error) -> {}, () -> Map.of(UdpTunnel.CHANNEL_VOICE, 24999));
        tunnel.start();
        try {
            for (int i = 0; i < 200 && tunnel.udpState() != Tunnel.UdpState.OK; i++) Thread.sleep(50);
            assertEquals(Tunnel.UdpState.OK, tunnel.udpState());
            assertEquals(System.getenv().getOrDefault("VIBEY_TEST_RELAY_VOICE_HOST", "24454"), tunnel.udpReady().voiceHost());
        } finally {
            tunnel.close();
        }
    }
}
