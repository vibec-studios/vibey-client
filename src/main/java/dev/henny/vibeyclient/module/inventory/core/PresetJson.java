package dev.henny.vibeyclient.module.inventory.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public final class PresetJson {
    private static final int MAX_ID = 128, MAX_NAME = ItemRef.MAX_NAME;

    private PresetJson() {}

    public static JsonObject write(PresetSet set) {
        JsonObject root = new JsonObject();
        root.addProperty("active", set.active());
        JsonArray list = new JsonArray();
        for (HotbarPreset p : set.stored()) list.add(write(p));
        root.add("presets", list);
        return root;
    }

    public static void read(JsonObject root, PresetSet into) {
        if (root == null) return;
        JsonArray list = root.has("presets") && root.get("presets").isJsonArray() ? root.getAsJsonArray("presets") : new JsonArray();
        for (int i = 0; i < PresetSet.MAX; i++) into.put(i, i < list.size() ? read(list.get(i)) : HotbarPreset.BLANK);
        JsonElement a = root.get("active");
        int active = a != null && a.isJsonPrimitive() && a.getAsJsonPrimitive().isNumber() ? a.getAsInt() : 0;
        into.select(Math.max(0, Math.min(into.size() - 1, active)));
    }

    private static JsonArray write(HotbarPreset p) {
        JsonArray slots = new JsonArray();
        for (ItemRef r : p.slots()) {
            JsonObject o = new JsonObject();
            if (!r.empty()) o.addProperty("id", r.id());
            if (r.name() != null) o.addProperty("name", r.name());
            slots.add(o);
        }
        return slots;
    }

    private static HotbarPreset read(JsonElement e) {
        if (e == null || !e.isJsonArray()) return HotbarPreset.BLANK;
        List<ItemRef> refs = new ArrayList<>();
        for (JsonElement s : e.getAsJsonArray()) refs.add(ref(s));
        return new HotbarPreset(refs);
    }

    private static ItemRef ref(JsonElement s) {
        if (s == null || !s.isJsonObject()) return ItemRef.EMPTY;
        JsonObject o = s.getAsJsonObject();
        String id = text(o, "id", MAX_ID);
        if (id == null || !id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) return ItemRef.EMPTY;
        return new ItemRef(id, text(o, "name", MAX_NAME));
    }

    private static String text(JsonObject o, String key, int max) {
        if (!o.has(key) || !o.get(key).isJsonPrimitive()) return null;
        String v = o.get(key).getAsString();
        return v.length() > max ? v.substring(0, max) : v;
    }
}
