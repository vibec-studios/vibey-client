package dev.henny.vibeyclient.net;

import dev.henny.vibeyclient.Compat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Equipped cosmetics, {@code slot → item id} ("" = empty). C→S: own loadout ({@code player} is ignored);
 * S→C: the loadout of {@code player}. An empty map means "nothing equipped / player left".
 */
public record LoadoutPayload(UUID player, Map<String, String> slots) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LoadoutPayload> TYPE = Compat.payloadType("loadout");

    public static final StreamCodec<FriendlyByteBuf, LoadoutPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeUUID(p.player);
                int n = Math.min(p.slots.size(), NetLimits.MAX_SLOTS);
                buf.writeVarInt(n);
                int i = 0;
                for (Map.Entry<String, String> e : p.slots.entrySet()) {
                    if (i++ >= n) break;
                    buf.writeUtf(e.getKey(), NetLimits.MAX_SLOT_NAME);
                    buf.writeUtf(e.getValue() == null ? "" : e.getValue(), NetLimits.MAX_ITEM_ID);
                }
            },
            buf -> {
                UUID player = buf.readUUID();
                int n = NetLimits.checkCount(buf.readVarInt(), NetLimits.MAX_SLOTS);
                Map<String, String> slots = new LinkedHashMap<>();
                for (int i = 0; i < n; i++) slots.put(buf.readUtf(NetLimits.MAX_SLOT_NAME), buf.readUtf(NetLimits.MAX_ITEM_ID));
                return new LoadoutPayload(player, slots);
            });

    public LoadoutPayload {
        slots = Collections.unmodifiableMap(new LinkedHashMap<>(slots));
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
