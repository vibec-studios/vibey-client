package dev.henny.vibeyclient.module.servers.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.LinkedHashSet;
import java.util.Set;

public final class Favourites {
    public static final int MAX = 256;

    private final Set<String> keys = new LinkedHashSet<>();

    public boolean contains(String address) {
        return keys.contains(AddressKey.of(address));
    }

    public boolean toggle(String address) {
        String k = AddressKey.of(address);
        if (k.isEmpty()) return false;
        if (keys.remove(k)) return false;
        if (keys.size() >= MAX) return false;
        keys.add(k);
        return true;
    }

    public int size() {
        return keys.size();
    }

    public JsonArray write() {
        JsonArray a = new JsonArray();
        for (String k : keys) a.add(k);
        return a;
    }

    public void read(JsonElement e) {
        keys.clear();
        if (e == null || !e.isJsonArray()) return;
        for (JsonElement k : e.getAsJsonArray()) {
            if (keys.size() >= MAX) break;
            if (!k.isJsonPrimitive()) continue;
            String v = AddressKey.of(k.getAsString());
            if (!v.isEmpty() && v.length() <= 255) keys.add(v);
        }
    }
}
