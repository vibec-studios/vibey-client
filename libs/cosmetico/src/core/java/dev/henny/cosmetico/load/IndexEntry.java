package dev.henny.cosmetico.load;

import com.google.gson.JsonObject;

public record IndexEntry(String file, String thumbnail, int sort) {
    public static IndexEntry of(JsonObject o) {
        String thumb = o.has("thumbnail") ? o.get("thumbnail").getAsString() : null;
        int sort = o.has("sort") ? o.get("sort").getAsInt() : 1000;
        return new IndexEntry(o.get("file").getAsString(), thumb, sort);
    }
}
