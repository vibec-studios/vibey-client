package dev.henny.vibeyclient.module.skins.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

final class Json {
    private Json() {}

    static String str(JsonObject o, String k) {
        JsonElement e = o.get(k);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) return null;
        String s = e.getAsString();
        return s.isEmpty() ? null : s;
    }

    static boolean bool(JsonObject o, String k, boolean def) {
        JsonElement e = o.get(k);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isBoolean()) return def;
        return e.getAsBoolean();
    }

    static long lng(JsonObject o, String k, long def) {
        JsonElement e = o.get(k);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) return def;
        return e.getAsLong();
    }

    static int integer(JsonObject o, String k, int def) {
        return (int) lng(o, k, def);
    }

    static JsonObject object(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }
}
