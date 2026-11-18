package dev.henny.vibeyclient.net;

import dev.henny.vibeyclient.Compat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/**
 * C→S: play/stop an emote ({@code player} and {@code startTick} are ignored). S→C: {@code player} plays/stops it,
 * {@code startTick} is the server tick it started at.
 */
public record EmotePayload(UUID player, String emoteId, boolean play, long startTick) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<EmotePayload> TYPE = Compat.payloadType("emote");

    public static final StreamCodec<FriendlyByteBuf, EmotePayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeUUID(p.player);
                buf.writeUtf(p.emoteId, NetLimits.MAX_ITEM_ID);
                buf.writeBoolean(p.play);
                buf.writeVarLong(p.startTick);
            },
            buf -> new EmotePayload(buf.readUUID(), buf.readUtf(NetLimits.MAX_ITEM_ID), buf.readBoolean(), buf.readVarLong()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
