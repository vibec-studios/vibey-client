package dev.henny.vibeyclient.module.clips.shots.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class Favourites {
    public static final int SCHEMA = 1;
    private final Set<String> names = new LinkedHashSet<>();

    public boolean contains(String name) {
        return names.contains(name);
    }

    public boolean toggle(String name) {
        if (names.remove(name)) return false;
        names.add(name);
        return true;
    }

    public void forget(String name) {
        names.remove(name);
    }

    public Set<String> names() {
        return Collections.unmodifiableSet(names);
    }

    public String toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("schema", SCHEMA);
        JsonArray a = new JsonArray();
        for (String n : names) a.add(n);
        o.add("favorites", a);
        return o.toString();
    }

    public static Favourites fromJson(String json) {
        Favourites f = new Favourites();
        if (json == null || json.isBlank()) return f;
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject() || !root.getAsJsonObject().has("favorites")) return f;
            for (JsonElement e : root.getAsJsonObject().getAsJsonArray("favorites")) {
                if (e.isJsonPrimitive()) f.names.add(e.getAsString());
            }
        } catch (RuntimeException ignored) {
            return new Favourites();
        }
        return f;
    }
}
