package dev.henny.vibeyclient.module.skins.core;

import com.google.gson.JsonObject;

public record SkinRequest(String id, String skinId, Boolean slim, long requestedAt) {
    public static final int SCHEMA = 1;

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("schema", SCHEMA);
        o.addProperty("id", id);
        o.addProperty("action", "apply");
        o.addProperty("skin_id", skinId);
        if (slim != null) o.addProperty("variant", slim ? "slim" : "classic");
        o.addProperty("requested_at", requestedAt);
        return o;
    }
}
