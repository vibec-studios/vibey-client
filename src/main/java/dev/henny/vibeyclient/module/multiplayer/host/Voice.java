package dev.henny.vibeyclient.module.multiplayer.host;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.module.multiplayer.MultiplayerModule;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

public final class Voice {
    public static final String MOD_ID = "voicechat";
    private static final int DEFAULT_PORT = 24454;

    private static volatile String voiceHost;
    private static volatile int localPort = -1;

    private Voice() {}

    public static boolean installed() {
        try {
            return Vibey.platform().isModLoaded(MOD_ID);
        } catch (RuntimeException e) {
            return false;
        }
    }

    static Map<Integer, Integer> channels(int lanPort) {
        if (!installed()) return Map.of();
        int port = svcPort(Vibey.platform().configDir().resolve(MOD_ID).resolve("voicechat-server.properties"), lanPort);
        localPort = port;
        return port > 0 ? Map.of(UdpTunnel.CHANNEL_VOICE, port) : Map.of();
    }

    static int svcPort(Path config, int lanPort) {
        int port = DEFAULT_PORT;
        if (Files.isRegularFile(config)) {
            Properties p = new Properties();
            try (Reader r = Files.newBufferedReader(config, StandardCharsets.UTF_8)) {
                p.load(r);
                String v = p.getProperty("port");
                if (v != null && !v.isBlank()) port = Integer.parseInt(v.trim());
            } catch (Exception e) {
                MultiplayerModule.LOG.debug("Could not read {}: {}", config, e.toString());
            }
        }
        if (port == -1) port = lanPort;
        return port > 0 && port <= 65535 ? port : -1;
    }

    public static int localPort() {
        return localPort;
    }

    public static String voiceHost() {
        return voiceHost;
    }

    public static boolean portMismatch() {
        String host = voiceHost;
        return host != null && !host.equals(Integer.toString(localPort));
    }

    static void onUdp(Tunnel.UdpState state, UdpReady ready) {
        voiceHost = ready != null && ready.voice() && state != Tunnel.UdpState.OFF ? ready.voiceHost() : null;
        if (portMismatch()) {
            MultiplayerModule.LOG.warn("Voice chat: local SVC port {} differs from the relay port {}", localPort, voiceHost);
        }
    }

    static void reset() {
        voiceHost = null;
    }
}
