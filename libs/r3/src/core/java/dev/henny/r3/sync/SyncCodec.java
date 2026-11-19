package dev.henny.r3.sync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.henny.r3.persist.PlacementCodec;
import dev.henny.r3.persist.PlacementRecord;

public final class SyncCodec {
    public static final int MAX_LENGTH = 8192;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private SyncCodec() {}

    public static String encode(SyncMessage m) {
        JsonObject o = new JsonObject();
        o.addProperty("op", m.op().name());
        o.addProperty("kind", m.kind());
        o.addProperty("id", m.objectId());
        o.addProperty("rev", m.revision());
        if (m.placement() != null) o.add("placement", GSON.toJsonTree(m.placement()));
        if (m.payload() != null) o.add("payload", m.payload());
        String json = GSON.toJson(o);
        if (json.length() > MAX_LENGTH) throw new IllegalArgumentException("sync message too large: " + json.length());
        return json;
    }

    public static SyncMessage decode(String json) {
        if (json == null || json.length() > MAX_LENGTH) throw new IllegalArgumentException("bad sync message size");
        try {
            JsonObject o = GSON.fromJson(json, JsonObject.class);
            SyncOp op = SyncOp.valueOf(o.get("op").getAsString());
            String kind = o.get("kind").getAsString();
            String id = o.get("id").getAsString();
            long rev = o.get("rev").getAsLong();
            PlacementRecord placement = o.has("placement") ? GSON.fromJson(o.get("placement"), PlacementRecord.class) : null;
            if (op == SyncOp.UPSERT && (!PlacementCodec.valid(placement) || !id.equals(placement.id))) {
                throw new IllegalArgumentException("bad placement");
            }
            JsonElement payload = o.get("payload");
            return new SyncMessage(op, kind, id, rev, placement, payload != null && payload.isJsonObject() ? payload.getAsJsonObject() : null);
        } catch (JsonParseException | IllegalStateException | NullPointerException | UnsupportedOperationException e) {
            throw new IllegalArgumentException("bad sync message", e);
        }
    }
}
