package dev.henny.vibeyclient.module.servers.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class OrderCodec {
    public static final int MAX = 4096;

    private OrderCodec() {}

    public static List<String> read(JsonElement e) {
        Set<String> out = new LinkedHashSet<>();
        if (e instanceof JsonObject o && o.get("order") instanceof JsonArray a) {
            for (JsonElement x : a) {
                if (out.size() >= MAX) break;
                if (x.isJsonPrimitive() && x.getAsJsonPrimitive().isString() && !x.getAsString().isBlank()) out.add(x.getAsString());
            }
        }
        return new ArrayList<>(out);
    }

    public static JsonObject write(List<String> order) {
        JsonObject o = new JsonObject();
        o.addProperty("version", 1);
        JsonArray a = new JsonArray();
        for (String id : order) a.add(id);
        o.add("order", a);
        return o;
    }
}
