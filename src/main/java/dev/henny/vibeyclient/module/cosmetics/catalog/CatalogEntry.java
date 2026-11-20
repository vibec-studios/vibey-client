package dev.henny.vibeyclient.module.cosmetics.catalog;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Locale;

record CatalogEntry(String id, String hash, String url, JsonObject json, String minClient) {
    static CatalogEntry of(JsonElement el) {
        if (!(el instanceof JsonObject o) || !(o.get("id") instanceof JsonPrimitive)) return null;
        String id = o.get("id").getAsString();
        JsonObject asset = o.get("asset") instanceof JsonObject x ? x : null;
        String hash = asset != null && asset.get("hash") instanceof JsonPrimitive h ? h.getAsString().toLowerCase(Locale.ROOT) : "";
        if (!hash.matches("[0-9a-f]{64}")) {
            CatalogLog.once(id + "@nohash", "Catalog item {} has no valid asset hash, skipped", id);
            return null;
        }
        String url = asset.get("url") instanceof JsonPrimitive u ? u.getAsString() : "/api/v1/cosmetics/assets/" + hash;
        String min = o.get("minClientVersion") instanceof JsonPrimitive m ? m.getAsString() : null;
        return new CatalogEntry(id, hash, url, o, min);
    }

    String string(String key) {
        return json.get(key) instanceof JsonPrimitive p ? p.getAsString() : null;
    }

    int sort() {
        return json.get("sort") instanceof JsonPrimitive s && s.isNumber() ? s.getAsInt() : 1000;
    }

    String availability() {
        String a = string("availability");
        return a == null ? "default" : a;
    }
}
