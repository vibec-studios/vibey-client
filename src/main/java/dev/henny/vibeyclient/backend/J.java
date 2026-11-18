package dev.henny.vibeyclient.backend;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Null-safe JSON accessors for the backend's (forward compatible, partly nullable) payloads. */
public final class J {
    private J() {}

    public static JsonObject obj(JsonElement e) {
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    public static JsonObject obj(JsonObject o, String key) {
        return o == null ? null : obj(o.get(key));
    }

    public static JsonArray arr(JsonElement e) {
        return e != null && e.isJsonArray() ? e.getAsJsonArray() : null;
    }

    public static JsonArray arr(JsonObject o, String key) {
        return o == null ? null : arr(o.get(key));
    }

    public static String str(JsonObject o, String key) {
        if (o == null) return null;
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive()) return null;
        return e.getAsString();
    }

    public static String str(JsonObject o, String key, String def) {
        String s = str(o, key);
        return s == null ? def : s;
    }

    public static int num(JsonObject o, String key, int def) {
        if (o == null) return def;
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) return def;
        return e.getAsInt();
    }

    public static long lng(JsonObject o, String key, long def) {
        if (o == null) return def;
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) return def;
        return e.getAsLong();
    }

    public static boolean bool(JsonObject o, String key, boolean def) {
        if (o == null) return def;
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isBoolean()) return def;
        return e.getAsBoolean();
    }

    /** Maps every object of an array (null-safe, skips non-objects). */
    public static <T> List<T> list(JsonElement e, Function<JsonObject, T> map) {
        List<T> out = new ArrayList<>();
        JsonArray a = arr(e);
        if (a == null) return out;
        for (JsonElement x : a) {
            if (x != null && x.isJsonObject()) {
                T t = map.apply(x.getAsJsonObject());
                if (t != null) out.add(t);
            }
        }
        return out;
    }

    public static JsonObject of(Object... kv) {
        JsonObject o = new JsonObject();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            String k = (String) kv[i];
            Object v = kv[i + 1];
            if (v == null) o.add(k, com.google.gson.JsonNull.INSTANCE);
            else if (v instanceof JsonElement je) o.add(k, je);
            else if (v instanceof String s) o.addProperty(k, s);
            else if (v instanceof Number n) o.addProperty(k, n);
            else if (v instanceof Boolean b) o.addProperty(k, b);
            else o.addProperty(k, v.toString());
        }
        return o;
    }

    public static JsonArray strings(Iterable<String> values) {
        JsonArray a = new JsonArray();
        for (String s : values) a.add(new JsonPrimitive(s));
        return a;
    }
}
