package dev.henny.vibeyclient.net;

import dev.henny.vibeyclient.Compat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/**
 * Generic envelope for module messages ({@code VibeyNet.channel(...)}): a channel name plus opaque bytes.
 * C→S: {@code peer} = target player, or {@link NetLimits#NOBODY} for "everyone else with the mod".
 * S→C: {@code peer} = the sender. The server only relays, it never looks inside {@code data}.
 */
public record MessagePayload(UUID peer, String channel, byte[] data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MessagePayload> TYPE = Compat.payloadType("msg");

    public static final StreamCodec<FriendlyByteBuf, MessagePayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeUUID(p.peer);
                buf.writeUtf(p.channel, NetLimits.MAX_CHANNEL);
                buf.writeByteArray(p.data);
            },
            buf -> new MessagePayload(buf.readUUID(), buf.readUtf(NetLimits.MAX_CHANNEL), buf.readByteArray(NetLimits.MAX_MESSAGE_BYTES)));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
