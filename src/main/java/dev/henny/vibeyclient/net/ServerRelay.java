package dev.henny.vibeyclient.net;

import dev.henny.vibeyclient.Compat;
import dev.henny.vibeyclient.Vibey;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server side of the {@code vibeyclient:*} channel (SPEC §6). Runs on dedicated servers and in the integrated server
 * (singleplayer/LAN/hosted worlds). It only validates sizes and rates and relays to every other player that sent
 * hello; it never knows what a cosmetic or emote is. All methods run on the server thread.
 */
public final class ServerRelay {
    private ServerRelay() {}

    private static final Map<UUID, Peer> PEERS = new ConcurrentHashMap<>();

    private static final class Peer {
        final ServerPlayer player;
        Map<String, String> loadout = Map.of();
        final RateLimit loadoutRate = new RateLimit(2, 6);
        final RateLimit emoteRate = new RateLimit(2, 4);
        final RateLimit messageRate = new RateLimit(20, 40);
        int dropped;

        Peer(ServerPlayer player) {
            this.player = player;
        }
    }

    public static void init() {
        PEERS.clear();
    }

    /** Players that said hello (have the mod), for debugging / other server code. */
    public static int peerCount() {
        return PEERS.size();
    }

    public static void handle(ServerPlayer from, CustomPacketPayload payload) {
        try {
            if (payload instanceof HelloPayload hello) onHello(from, hello);
            else {
                Peer peer = PEERS.get(from.getUUID());
                if (peer == null || peer.player != from) return; // must say hello first
                if (payload instanceof LoadoutPayload p) onLoadout(peer, p);
                else if (payload instanceof EmotePayload p) onEmote(peer, p);
                else if (payload instanceof MessagePayload p) onMessage(peer, p);
            }
        } catch (RuntimeException e) {
            Vibey.LOG.warn("Dropped a bad vibeyclient packet from {}: {}", from.getName().getString(), e.toString());
        }
    }

    public static void onLeave(ServerPlayer player) {
        Peer peer = PEERS.get(player.getUUID());
        if (peer == null || peer.player != player) return;
        PEERS.remove(player.getUUID());
        if (!peer.loadout.isEmpty()) broadcast(peer, new LoadoutPayload(player.getUUID(), Map.of()));
    }

    public static void onServerStopped() {
        PEERS.clear();
    }

    // ============================================================================================ handlers

    private static void onHello(ServerPlayer from, HelloPayload hello) {
        Peer peer = new Peer(from);
        PEERS.put(from.getUUID(), peer);
        send(from, new HelloPayload(NetLimits.PROTOCOL));
        // Current loadouts of everyone else, so the new player sees them right away.
        for (Peer other : PEERS.values()) {
            if (other != peer && !other.loadout.isEmpty()) send(from, new LoadoutPayload(other.player.getUUID(), other.loadout));
        }
    }

    private static void onLoadout(Peer peer, LoadoutPayload p) {
        if (!peer.loadoutRate.tryTake()) {
            drop(peer, "loadout rate");
            return;
        }
        Map<String, String> clean = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : p.slots().entrySet()) {
            if (!NetLimits.SLOT.matcher(e.getKey()).matches() || !NetLimits.ITEM_ID.matcher(e.getValue()).matches()) {
                drop(peer, "bad loadout entry");
                return;
            }
            clean.put(e.getKey(), e.getValue());
        }
        peer.loadout = Collections.unmodifiableMap(clean);
        broadcast(peer, new LoadoutPayload(peer.player.getUUID(), peer.loadout));
    }

    private static void onEmote(Peer peer, EmotePayload p) {
        if (!peer.emoteRate.tryTake()) {
            drop(peer, "emote rate");
            return;
        }
        if (p.emoteId().isEmpty() || !NetLimits.ITEM_ID.matcher(p.emoteId()).matches()) {
            drop(peer, "bad emote id");
            return;
        }
        MinecraftServer server = Compat.server(peer.player);
        broadcast(peer, new EmotePayload(peer.player.getUUID(), p.emoteId(), p.play(), server.getTickCount()));
    }

    private static void onMessage(Peer peer, MessagePayload p) {
        if (!peer.messageRate.tryTake()) {
            drop(peer, "message rate");
            return;
        }
        if (!NetLimits.CHANNEL.matcher(p.channel()).matches()) {
            drop(peer, "bad channel");
            return;
        }
        MessagePayload out = new MessagePayload(peer.player.getUUID(), p.channel(), p.data());
        if (NetLimits.NOBODY.equals(p.peer())) {
            broadcast(peer, out);
        } else {
            Peer target = PEERS.get(p.peer());
            if (target != null && target != peer) send(target.player, out);
        }
    }

    // ============================================================================================ util

    private static void broadcast(Peer from, CustomPacketPayload payload) {
        for (Peer other : PEERS.values()) {
            if (other != from) send(other.player, payload);
        }
    }

    private static void send(ServerPlayer to, CustomPacketPayload payload) {
        try {
            Vibey.platform().sendToPlayer(to, payload);
        } catch (RuntimeException e) {
            Vibey.LOG.debug("Could not send {} to {}: {}", payload.type().id(), to.getName().getString(), e.toString());
        }
    }

    private static void drop(Peer peer, String why) {
        // Log only the first few per player so a spamming client cannot flood the log.
        if (peer.dropped++ < 5) Vibey.LOG.info("vibeyclient: dropped packet from {} ({})", peer.player.getName().getString(), why);
    }

    /** Token bucket: {@code perSecond} sustained, {@code burst} at once. */
    static final class RateLimit {
        private final double perSecond;
        private final double burst;
        private double tokens;
        private long last = System.nanoTime();

        RateLimit(double perSecond, double burst) {
            this.perSecond = perSecond;
            this.burst = burst;
            this.tokens = burst;
        }

        boolean tryTake() {
            long now = System.nanoTime();
            tokens = Math.min(burst, tokens + (now - last) / 1e9 * perSecond);
            last = now;
            if (tokens < 1) return false;
            tokens -= 1;
            return true;
        }
    }
}
