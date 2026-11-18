package dev.henny.vibeyclient.module.multiplayer.net;

import dev.henny.vibeyclient.module.multiplayer.MpCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Joining player -> host: "these mods are installed on my side" (entries are {@code id@version}), for "Force mods".
 * Our own id is {@code vibeyclient:mp_modlist}. {@link Legacy} is the same packet under the id of the standalone Vibey
 * Multiplayer mod ({@code vibey:mods}), so hosts and guests of both mods understand each other; it is only registered
 * while that mod is not installed (then this module is off anyway).
 */
public record ModListPayload(List<String> mods) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ModListPayload> TYPE = MpCompat.payloadType("vibeyclient", "mp_modlist");
    private static final int MAX = 1024;

    public static final StreamCodec<FriendlyByteBuf, ModListPayload> CODEC = StreamCodec.of(
            (buf, p) -> write(buf, p.mods), buf -> new ModListPayload(read(buf)));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Wire-compatible with {@code vibey:mods} of the standalone mod. */
    public record Legacy(List<String> mods) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<Legacy> TYPE = MpCompat.payloadType("vibey", "mods");
        public static final StreamCodec<FriendlyByteBuf, Legacy> CODEC = StreamCodec.of(
                (buf, p) -> write(buf, p.mods), buf -> new Legacy(read(buf)));

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    static void write(FriendlyByteBuf buf, List<String> mods) {
        List<String> list = mods.size() > MAX ? mods.subList(0, MAX) : mods;
        buf.writeVarInt(list.size());
        for (String s : list) buf.writeUtf(s, 256);
    }

    static List<String> read(FriendlyByteBuf buf) {
        int n = Math.min(buf.readVarInt(), MAX);
        List<String> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) list.add(buf.readUtf(256));
        return list;
    }
}
