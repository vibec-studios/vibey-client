package dev.henny.vibeyclient.api.net;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.event.Events;
import dev.henny.vibeyclient.api.event.VibeyEvents;
import dev.henny.vibeyclient.client.VibeyCore;
import dev.henny.vibeyclient.net.EmotePayload;
import dev.henny.vibeyclient.net.HelloPayload;
import dev.henny.vibeyclient.net.LoadoutPayload;
import dev.henny.vibeyclient.net.MessagePayload;
import dev.henny.vibeyclient.net.NetLimits;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * Client side of the server channel (SPEC §6). Works only on servers that also run Vibey Client (dedicated, LAN or
 * the integrated server); on every other server {@link #available()} stays false and all sends return false.
 * Incoming loadouts/emotes arrive as {@link VibeyEvents.RemoteLoadout} / {@link VibeyEvents.RemoteEmote} events.
 * Client thread only.
 */
public final class VibeyNet {
    private VibeyNet() {}

    private static volatile int serverProtocol = -1;
    private static final Map<String, Channel<?>> CHANNELS = new ConcurrentHashMap<>();

    /** The server answered our hello. */
    public static boolean available() {
        return serverProtocol >= 0;
    }

    public static int serverProtocol() {
        return serverProtocol;
    }

    /** Sends our loadout ({@code slot → item id}, "" = empty) to everyone on the server who has the mod. */
    public static boolean sendLoadout(Map<String, String> slots) {
        return available() && send(new LoadoutPayload(NetLimits.NOBODY, slots));
    }

    public static boolean sendEmote(String emoteId, boolean play) {
        return available() && send(new EmotePayload(NetLimits.NOBODY, emoteId, play, 0));
    }

    /**
     * A typed module message channel, e.g. {@code VibeyNet.channel("social:wave", MyMsg.CODEC)}. Name: lower case,
     * {@code [a-z0-9_.:-]}, max 64 chars; encoded size max 8 KiB. Create it once (onLoad).
     */
    public static <T> Channel<T> channel(String name, StreamCodec<FriendlyByteBuf, T> codec) {
        if (!NetLimits.CHANNEL.matcher(name).matches()) throw new IllegalArgumentException("Bad channel name " + name);
        Channel<T> c = new Channel<>(name, codec);
        if (CHANNELS.putIfAbsent(name, c) != null) throw new IllegalArgumentException("Channel exists: " + name);
        return c;
    }

    public static final class Channel<T> {
        private final String name;
        private final StreamCodec<FriendlyByteBuf, T> codec;
        private final List<BiConsumer<UUID, T>> handlers = new CopyOnWriteArrayList<>();

        private Channel(String name, StreamCodec<FriendlyByteBuf, T> codec) {
            this.name = name;
            this.codec = codec;
        }

        public String name() {
            return name;
        }

        /** To every other player with the mod. */
        public boolean broadcast(T message) {
            return sendTo(NetLimits.NOBODY, message);
        }

        /** To one player (if they have the mod). */
        public boolean sendTo(UUID player, T message) {
            if (!available()) return false;
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            try {
                codec.encode(buf, message);
                if (buf.readableBytes() > NetLimits.MAX_MESSAGE_BYTES) throw new IllegalArgumentException("Message too big for " + name);
                byte[] data = new byte[buf.readableBytes()];
                buf.readBytes(data);
                return send(new MessagePayload(player, name, data));
            } finally {
                buf.release();
            }
        }

        /** {@code handler(sender, message)} on the client thread. */
        public Channel<T> onReceive(BiConsumer<UUID, T> handler) {
            handlers.add(handler);
            return this;
        }

        void receive(UUID sender, byte[] data) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
            T msg;
            try {
                msg = codec.decode(buf);
            } catch (RuntimeException e) {
                Vibey.LOG.warn("Bad message on {} from {}: {}", name, sender, e.toString());
                return;
            } finally {
                buf.release();
            }
            for (BiConsumer<UUID, T> h : handlers) {
                try {
                    h.accept(sender, msg);
                } catch (RuntimeException e) {
                    Vibey.LOG.error("Handler of {} failed", name, e);
                }
            }
        }
    }

    // ============================================================================================ core plumbing

    private static boolean send(CustomPacketPayload payload) {
        try {
            if (!VibeyCore.clientPlatform().canSendToServer(payload.type())) return false;
            VibeyCore.clientPlatform().sendToServer(payload);
            return true;
        } catch (RuntimeException e) {
            Vibey.LOG.debug("Could not send {}: {}", payload.type().id(), e.toString());
            return false;
        }
    }

    /** Core: joined a world. Says hello if the server knows the channel. */
    public static void onJoin() {
        serverProtocol = -1;
        send(new HelloPayload(NetLimits.PROTOCOL));
    }

    public static void onLeave() {
        serverProtocol = -1;
    }

    /** Core: a payload from the server (client thread). */
    public static void handle(CustomPacketPayload payload) {
        if (payload instanceof HelloPayload p) {
            serverProtocol = p.protocol();
            Vibey.LOG.info("Server has Vibey Client (protocol {})", p.protocol());
            Events.post(new VibeyEvents.ServerHello(p.protocol()));
        } else if (payload instanceof LoadoutPayload p) {
            Events.post(new VibeyEvents.RemoteLoadout(p.player(), p.slots()));
        } else if (payload instanceof EmotePayload p) {
            Events.post(new VibeyEvents.RemoteEmote(p.player(), p.emoteId(), p.play(), p.startTick()));
        } else if (payload instanceof MessagePayload p) {
            Channel<?> c = CHANNELS.get(p.channel());
            if (c != null) c.receive(p.peer(), p.data());
        }
    }
}
