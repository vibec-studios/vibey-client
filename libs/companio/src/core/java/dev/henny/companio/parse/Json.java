package dev.henny.companio.parse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.henny.companio.def.Range;
import dev.henny.companio.expr.Suggest;
import dev.henny.companio.pack.Problems;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

final class Json {
    private Json() {}

    static String at(String path, String key) {
        return path.isEmpty() ? key : path + "." + key;
    }

    static String at(String path, int index) {
        return path + "[" + index + "]";
    }

    static void known(JsonObject o, String path, Problems p, String... keys) {
        List<String> allowed = Arrays.asList(keys);
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            if (!allowed.contains(e.getKey())) p.warn(at(path, e.getKey()), "unknown field \"" + e.getKey() + "\"" + Suggest.hint(e.getKey(), allowed));
        }
    }

    static String str(JsonObject o, String key, String path, Problems p, String def) {
        JsonElement e = o.get(key);
        if (e == null || e.isJsonNull()) return def;
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) return e.getAsString();
        p.error(at(path, key), "must be a text");
        return def;
    }

    static float num(JsonObject o, String key, String path, Problems p, float def, float min, float max) {
        JsonElement e = o.get(key);
        if (e == null || e.isJsonNull()) return def;
        if (!isNumber(e)) {
            p.error(at(path, key), "must be a number");
            return def;
        }
        float v = e.getAsFloat();
        if (v < min || v > max || Float.isNaN(v)) {
            p.error(at(path, key), "must be between " + fmt(min) + " and " + fmt(max) + ", is " + fmt(v));
            return Math.max(min, Math.min(max, Float.isNaN(v) ? def : v));
        }
        return v;
    }

    static boolean bool(JsonObject o, String key, String path, Problems p, boolean def) {
        JsonElement e = o.get(key);
        if (e == null || e.isJsonNull()) return def;
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean()) return e.getAsBoolean();
        p.error(at(path, key), "must be true or false");
        return def;
    }

    static JsonObject obj(JsonObject o, String key, String path, Problems p) {
        JsonElement e = o.get(key);
        if (e == null || e.isJsonNull()) return null;
        if (e.isJsonObject()) return e.getAsJsonObject();
        p.error(at(path, key), "must be an object { … }");
        return null;
    }

    static JsonArray arr(JsonObject o, String key, String path, Problems p) {
        JsonElement e = o.get(key);
        if (e == null || e.isJsonNull()) return null;
        if (e.isJsonArray()) return e.getAsJsonArray();
        p.error(at(path, key), "must be a list [ … ]");
        return null;
    }

    static Range range(JsonObject o, String key, String path, Problems p, Range def, float min, float max) {
        JsonElement e = o.get(key);
        if (e == null || e.isJsonNull()) return def;
        if (isNumber(e)) return Range.of(clamp(e.getAsFloat(), at(path, key), p, min, max));
        if (e.isJsonArray() && e.getAsJsonArray().size() == 2 && isNumber(e.getAsJsonArray().get(0)) && isNumber(e.getAsJsonArray().get(1))) {
            float a = clamp(e.getAsJsonArray().get(0).getAsFloat(), at(path, key), p, min, max);
            float b = clamp(e.getAsJsonArray().get(1).getAsFloat(), at(path, key), p, min, max);
            if (b < a) p.error(at(path, key), "[min, max] with max smaller than min");
            return new Range(Math.min(a, b), Math.max(a, b));
        }
        p.error(at(path, key), "must be a number or [min, max]");
        return def;
    }

    static float[] vec3(JsonObject o, String key, String path, Problems p, float[] def, float limit) {
        JsonElement e = o.get(key);
        if (e == null || e.isJsonNull()) return def;
        if (!e.isJsonArray() || e.getAsJsonArray().size() != 3) {
            p.error(at(path, key), "must be [x, y, z]");
            return def;
        }
        float[] v = new float[3];
        for (int i = 0; i < 3; i++) {
            JsonElement c = e.getAsJsonArray().get(i);
            if (!isNumber(c)) {
                p.error(at(path, key), "must be [x, y, z] numbers");
                return def;
            }
            v[i] = clamp(c.getAsFloat(), at(path, key), p, -limit, limit);
        }
        return v;
    }

    static boolean isNumber(JsonElement e) {
        return e != null && e.isJsonPrimitive() && ((JsonPrimitive) e).isNumber();
    }

    static boolean isString(JsonElement e) {
        return e != null && e.isJsonPrimitive() && ((JsonPrimitive) e).isString();
    }

    private static float clamp(float v, String path, Problems p, float min, float max) {
        if (v < min || v > max || Float.isNaN(v)) {
            p.error(path, "must be between " + fmt(min) + " and " + fmt(max) + ", is " + fmt(v));
            return Math.max(min, Math.min(max, Float.isNaN(v) ? min : v));
        }
        return v;
    }

    static String fmt(float v) {
        return v == Math.rint(v) && Math.abs(v) < 1e9 ? Long.toString((long) v) : Float.toString(v);
    }
}
