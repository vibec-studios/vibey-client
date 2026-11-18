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
 * <p>{@code token} (protocol ≥ {@link NetLimits#PROTOCOL_TOKEN}, "" = none) is the backend-signed loadout
 * ({@link LoadoutToken}, SPEC §6). On the wire it follows the slots and is only written when non-empty, so a packet
 * without a token is byte-identical to protocol 1. Senders only attach it when the other side said protocol ≥ 2
 * (older decoders would reject the extra bytes); the decoder reads it when bytes are left.
 */
public record LoadoutPayload(UUID player, Map<String, String> slots, String token) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LoadoutPayload> TYPE = Compat.payloadType("loadout");

    public static final StreamCodec<FriendlyByteBuf, LoadoutPayload> CODEC = StreamCodec.of(LoadoutPayload::write, LoadoutPayload::read);

    public LoadoutPayload {
        slots = Collections.unmodifiableMap(new LinkedHashMap<>(slots));
        token = token == null ? "" : token;
    }

    public LoadoutPayload(UUID player, Map<String, String> slots) {
        this(player, slots, "");
    }

    /** The same packet without the token (for peers below protocol 2). */
    public LoadoutPayload withoutToken() {
        return token.isEmpty() ? this : new LoadoutPayload(player, slots, "");
    }

    static void write(FriendlyByteBuf buf, LoadoutPayload p) {
        buf.writeUUID(p.player);
        int n = Math.min(p.slots.size(), NetLimits.MAX_SLOTS);
        buf.writeVarInt(n);
        int i = 0;
        for (Map.Entry<String, String> e : p.slots.entrySet()) {
            if (i++ >= n) break;
            buf.writeUtf(e.getKey(), NetLimits.MAX_SLOT_NAME);
            buf.writeUtf(e.getValue() == null ? "" : e.getValue(), NetLimits.MAX_ITEM_ID);
        }
        if (!p.token.isEmpty()) buf.writeUtf(p.token, LoadoutToken.MAX_LENGTH);
    }

    static LoadoutPayload read(FriendlyByteBuf buf) {
        UUID player = buf.readUUID();
        int n = NetLimits.checkCount(buf.readVarInt(), NetLimits.MAX_SLOTS);
        Map<String, String> slots = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) slots.put(buf.readUtf(NetLimits.MAX_SLOT_NAME), buf.readUtf(NetLimits.MAX_ITEM_ID));
        String token = buf.isReadable() ? buf.readUtf(LoadoutToken.MAX_LENGTH) : "";
        return new LoadoutPayload(player, slots, token);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
