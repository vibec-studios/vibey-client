package dev.henny.vibeyclient.net;

import dev.henny.vibeyclient.Compat;
import dev.henny.vibeyclient.Vibey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server side of the open server-rules channels on a Fabric/NeoForge <b>dedicated</b> server running this jar
 * ({@code docs/SERVER-RULES.md}): reads {@code config/vibeyclient-server.json}, sends {@code vibey:rules} to every
 * player whose client registered the channel (on join, after their {@code vibey:hello}, after a world change when
 * per-world rules exist, after {@code /vibeyrules reload}) and logs who announced {@code vibey:hello}. Integrated
 * servers (singleplayer/LAN) send nothing. All methods run on the server thread.
 */
public final class ServerRulesHost {
    private ServerRulesHost() {}

    public static final String CONFIG_FILE = "vibeyclient-server.json";

    private static final class Peer {
        /** Last JSON sent to the player (null = nothing sent yet). */
        byte[] sent;
        String world;
        RulesProtocol.Hello hello;
    }

    private static final Map<UUID, Peer> PEERS = new ConcurrentHashMap<>();
    private static volatile ServerRulesConfig config = ServerRulesConfig.EMPTY;
    private static volatile boolean active;

    public static boolean active() {
        return active;
    }

    public static ServerRulesConfig config() {
        return config;
    }

    private static Path file() {
        return Vibey.platform().configDir().resolve(CONFIG_FILE);
    }

    public static void serverStarted(MinecraftServer server) {
        PEERS.clear();
        active = server.isDedicatedServer() && RulesPayloads.active();
        if (!active) return;
        reload();
    }

    public static void serverStopped() {
        active = false;
        PEERS.clear();
    }

    /** (Re)reads the config file (writes the commented default on first start). Returns a status line. */
    public static String reload() {
        ServerRulesConfig.Loaded l = ServerRulesConfig.load(file());
        config = l.config();
        if (l.error() != null) Vibey.LOG.warn("[vibey rules] {}", l.error());
        String line = "Server rules: " + (config.enabled() ? config.disable().size() + " disabled key(s)"
                + (config.worlds().isEmpty() ? "" : ", " + config.worlds().size() + " world override(s)") : "off")
                + (l.error() != null ? " (" + l.error() + ")" : "");
        Vibey.LOG.info("[vibey rules] {}", line);
        return line;
    }

    /** Re-sends to everyone online (after a reload). */
    public static void resendAll(MinecraftServer server) {
        if (!active) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) send(p);
    }

    public static void onJoin(ServerPlayer player) {
        if (!active) return;
        PEERS.put(player.getUUID(), new Peer());
        send(player);
    }

    public static void onLeave(ServerPlayer player) {
        PEERS.remove(player.getUUID());
    }

    public static void onHello(ServerPlayer player, RulesPayloads.Hello payload) {
        if (!active) return;
        String name = player.getName().getString();
        RulesProtocol.Hello hello;
        try {
            hello = RulesProtocol.parseHello(payload.json());
        } catch (RulesProtocol.BadPacket e) {
            Vibey.LOG.warn("[vibey rules] Bad vibey:hello from {}: {}", name, e.getMessage());
            return;
        }
        Peer peer = PEERS.computeIfAbsent(player.getUUID(), k -> new Peer());
        boolean first = peer.hello == null;
        peer.hello = hello;
        if (first && config.logHello()) {
            Vibey.LOG.info("[vibey rules] {} announced vibey:hello: {} {}, supports {}", name, hello.client(), hello.version(), hello.supports());
        }
        // the client may have registered vibey:rules only now (e.g. late channel registration): try again
        send(player);
    }

    /** Once a second: per-world rules follow players between dimensions. */
    public static void tick(MinecraftServer server) {
        if (!active || config.worlds().isEmpty() || server.getTickCount() % 20 != 0) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Peer peer = PEERS.get(p.getUUID());
            if (peer != null && peer.world != null && !peer.world.equals(Compat.dimensionId(p))) send(p);
        }
    }

    /** Sends the player's rules if they changed since the last send (and the client can receive them). */
    static void send(ServerPlayer player) {
        Peer peer = PEERS.computeIfAbsent(player.getUUID(), k -> new Peer());
        String world = Compat.dimensionId(player);
        peer.world = world;
        RulesProtocol.Rules rules = config.rulesFor(world, player.getUUID(), player.getName().getString());
        // nothing to say, and nothing said before that needs clearing
        if (rules.isEmpty() && peer.sent == null) return;
        byte[] json = RulesProtocol.encodeRules(rules);
        if (peer.sent != null && Arrays.equals(peer.sent, json)) return;
        if (!Vibey.platform().canSendToPlayer(player, RulesPayloads.Rules.TYPE)) return;
        Vibey.platform().sendToPlayer(player, new RulesPayloads.Rules(json));
        peer.sent = json;
    }

    /** For {@code /vibeyrules status}. */
    public static List<String> status(MinecraftServer server) {
        List<String> out = new java.util.ArrayList<>();
        ServerRulesConfig c = config;
        out.add("Vibey server rules: " + (c.enabled() ? "on" : "off") + ", disable " + c.disable()
                + (c.message() == null ? "" : ", message \"" + c.message() + "\""));
        for (Map.Entry<String, ServerRulesConfig.World> w : c.worlds().entrySet()) {
            out.add("  world " + w.getKey() + ": disable " + w.getValue().disable() + (w.getValue().message() == null ? "" : ", message \"" + w.getValue().message() + "\""));
        }
        int announced = 0, sent = 0;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Peer peer = PEERS.get(p.getUUID());
            if (peer == null) continue;
            if (peer.hello != null) {
                announced++;
                out.add("  " + p.getName().getString() + ": " + peer.hello.client() + " " + peer.hello.version() + ", " + peer.hello.supports().size() + " supported key(s)"
                        + (peer.sent != null ? ", rules sent" : ""));
            }
            if (peer.sent != null) sent++;
        }
        out.add(announced + " player(s) announced vibey:hello, rules sent to " + sent);
        return out;
    }
}
