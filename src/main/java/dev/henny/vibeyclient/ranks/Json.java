package dev.henny.vibeyclient.ranks;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;

final class Json {
    private Json() {}

    static List<JsonObject> objects(JsonElement e) {
        List<JsonObject> out = new ArrayList<>();
        if (e instanceof JsonArray a) for (JsonElement x : a) if (x instanceof JsonObject o) out.add(o);
        return out;
    }

    static List<String> strings(JsonElement e) {
        List<String> out = new ArrayList<>();
        if (e instanceof JsonArray a) for (JsonElement x : a) if (x instanceof JsonPrimitive p && p.isString()) out.add(p.getAsString());
        return List.copyOf(out);
    }

    static String text(JsonObject o, String key, String def) {
        return o != null && o.get(key) instanceof JsonPrimitive p && p.isString() ? p.getAsString() : def;
    }

    static int num(JsonObject o, String key) {
        return o != null && o.get(key) instanceof JsonPrimitive p && p.isNumber() ? p.getAsInt() : 0;
    }

    static boolean bool(JsonObject o, String key, boolean def) {
        return o != null && o.get(key) instanceof JsonPrimitive p && p.isBoolean() ? p.getAsBoolean() : def;
    }

    static JsonObject obj(JsonObject o, String key) {
        return o != null && o.get(key) instanceof JsonObject x ? x : null;
    }
}
