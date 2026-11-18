package dev.henny.vibeyclient.net;

import dev.henny.vibeyclient.Compat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** C→S: "I have Vibey Client, protocol N". S→C: the ack with the protocol of the server. */
public record HelloPayload(int protocol) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<HelloPayload> TYPE = Compat.payloadType("hello");

    public static final StreamCodec<FriendlyByteBuf, HelloPayload> CODEC = StreamCodec.of(
            (buf, p) -> buf.writeVarInt(p.protocol),
            buf -> new HelloPayload(buf.readVarInt()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
