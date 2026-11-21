package dev.henny.vibeyclient.net;

import dev.henny.vibeyclient.Compat;
import dev.henny.vibeyclient.Vibey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The two open server-rules channels ({@link RulesProtocol}): raw JSON bytes as the whole payload body, so Paper /
 * Spigot plugins ({@code player.sendPluginMessage}) and any other server software can read and write them.
 *
 * <p>Namespace {@code vibey} on purpose (it is the documented, client-neutral protocol). The standalone Vibey
 * Multiplayer mod (mod id {@code vibey}) only registers {@code vibey:mods}; should another mod ever register one of
 * these ids first, the loaders' registration throws and {@link #disable} switches the feature off instead of crashing.
 */
public final class RulesPayloads {
    private RulesPayloads() {}

    private static volatile boolean disabled;

    /** False if another mod already owns {@code vibey:rules}/{@code vibey:hello} (registration failed). */
    public static boolean active() {
        return !disabled;
    }

    public static void disable(RuntimeException cause) {
        disabled = true;
        Vibey.LOG.warn("Server rules channels (vibey:rules / vibey:hello) are taken by another mod, server rules are off: {}", cause.toString());
    }

    /** Server → client: the rules JSON. {@code json} is null when the packet was too big (dropped by the reader). */
    public record Rules(byte[] json) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<Rules> TYPE = new CustomPacketPayload.Type<>(Compat.id("vibey", "rules"));
        public static final StreamCodec<FriendlyByteBuf, Rules> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeBytes(p.json), buf -> new Rules(readAll(buf)));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client → server: the hello JSON (see {@link RulesProtocol#encodeHello}). */
    public record Hello(byte[] json) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<Hello> TYPE = new CustomPacketPayload.Type<>(Compat.id("vibey", "hello"));
        public static final StreamCodec<FriendlyByteBuf, Hello> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeBytes(p.json), buf -> new Hello(readAll(buf)));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Presentation(byte[] json) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<Presentation> TYPE =
                new CustomPacketPayload.Type<>(Compat.id("vibey", "presentation"));
        public static final StreamCodec<FriendlyByteBuf, Presentation> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeBytes(p.json), buf -> new Presentation(readAll(buf)));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Reads the whole rest of the payload; always consumes it (leftover bytes would kick the player). */
    static byte[] readAll(FriendlyByteBuf buf) {
        int n = buf.readableBytes();
        if (n > RulesProtocol.MAX_BYTES) {
            buf.skipBytes(n);
            return null;
        }
        byte[] data = new byte[n];
        buf.readBytes(data);
        return data;
    }
}
