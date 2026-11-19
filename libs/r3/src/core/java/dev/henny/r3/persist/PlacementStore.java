package dev.henny.r3.persist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PlacementStore {
    public static final int FORMAT = 1;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private final Map<String, LinkedHashMap<String, PlacementRecord>> worlds = new LinkedHashMap<>();

    public void put(String worldKey, PlacementRecord record) {
        worlds.computeIfAbsent(worldKey, k -> new LinkedHashMap<>()).put(record.id, record);
    }

    public PlacementRecord get(String worldKey, String id) {
        Map<String, PlacementRecord> w = worlds.get(worldKey);
        return w == null ? null : w.get(id);
    }

    public boolean remove(String worldKey, String id) {
        Map<String, PlacementRecord> w = worlds.get(worldKey);
        if (w == null || w.remove(id) == null) return false;
        if (w.isEmpty()) worlds.remove(worldKey);
        return true;
    }

    public List<PlacementRecord> list(String worldKey) {
        Map<String, PlacementRecord> w = worlds.get(worldKey);
        return w == null ? List.of() : List.copyOf(w.values());
    }

    public List<PlacementRecord> list(String worldKey, String kind) {
        List<PlacementRecord> out = new ArrayList<>();
        for (PlacementRecord r : list(worldKey)) if (kind.equals(r.kind)) out.add(r);
        return out;
    }

    public void replaceAll(String worldKey, String kind, Collection<PlacementRecord> records) {
        Map<String, PlacementRecord> w = worlds.computeIfAbsent(worldKey, k -> new LinkedHashMap<>());
        w.values().removeIf(r -> kind.equals(r.kind));
        for (PlacementRecord r : records) w.put(r.id, r);
        if (w.isEmpty()) worlds.remove(worldKey);
    }

    public void clear(String worldKey) {
        worlds.remove(worldKey);
    }

    public Set<String> worldKeys() {
        return worlds.keySet();
    }

    public JsonObject toJsonTree() {
        JsonObject root = new JsonObject();
        root.addProperty("format", FORMAT);
        JsonObject all = new JsonObject();
        worlds.forEach((key, records) -> {
            JsonArray arr = new JsonArray();
            records.values().forEach(r -> arr.add(GSON.toJsonTree(r)));
            all.add(key, arr);
        });
        root.add("worlds", all);
        return root;
    }

    public String toJson() {
        return GSON.toJson(toJsonTree());
    }

    public static PlacementStore fromJson(String json) {
        PlacementStore store = new PlacementStore();
        if (json == null || json.isBlank()) return store;
        try {
            JsonElement root = GSON.fromJson(json, JsonElement.class);
            if (root != null && root.isJsonObject()) store.load(root.getAsJsonObject());
        } catch (JsonParseException e) {
            return store;
        }
        return store;
    }

    public static PlacementStore fromJsonTree(JsonObject root) {
        PlacementStore store = new PlacementStore();
        if (root != null) store.load(root);
        return store;
    }

    private void load(JsonObject root) {
        JsonElement all = root.get("worlds");
        if (all == null || !all.isJsonObject()) return;
        for (Map.Entry<String, JsonElement> world : all.getAsJsonObject().entrySet()) {
            if (!world.getValue().isJsonArray()) continue;
            for (JsonElement e : world.getValue().getAsJsonArray()) {
                PlacementRecord r = parse(e);
                if (r != null) put(world.getKey(), r);
            }
        }
    }

    private static PlacementRecord parse(JsonElement e) {
        try {
            PlacementRecord r = GSON.fromJson(e, PlacementRecord.class);
            return PlacementCodec.valid(r) ? r : null;
        } catch (JsonParseException | IllegalStateException | NumberFormatException ex) {
            return null;
        }
    }
}
