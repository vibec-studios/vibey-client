package dev.henny.vibeyclient.ui.hud;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.TreeSet;

final class StateJson {
    private StateJson() {}

    static JsonObject write(HudLayout.State s, boolean onlyCustom) {
        JsonObject root = new JsonObject();
        root.addProperty("v", 1);
        JsonObject g = s.global.toJson();
        if (g.size() > 0) root.add("g", g);
        JsonObject els = new JsonObject();
        for (Map.Entry<String, HudLayout.Placement> en : s.placements.entrySet()) {
            if (onlyCustom && !en.getValue().custom) continue;
            els.add(en.getKey(), PlacementJson.write(en.getValue()));
        }
        for (Map.Entry<String, JsonObject> en : s.unknown.entrySet()) if (!els.has(en.getKey())) els.add(en.getKey(), en.getValue());
        root.add("e", els);
        if (!s.copies.isEmpty()) root.add("c", copiesJson(s.copies));
        return root;
    }

    static JsonObject copiesJson(Map<String, TreeSet<Integer>> copies) {
        JsonObject c = new JsonObject();
        for (Map.Entry<String, TreeSet<Integer>> en : copies.entrySet()) {
            JsonArray arr = new JsonArray();
            for (int i : en.getValue()) arr.add(i);
            c.add(en.getKey(), arr);
        }
        return c;
    }

    static TreeSet<Integer> readCopies(JsonElement el) {
        TreeSet<Integer> set = new TreeSet<>();
        if (el == null || !el.isJsonArray()) return set;
        for (JsonElement i : el.getAsJsonArray()) {
            if (!i.isJsonPrimitive() || !i.getAsJsonPrimitive().isNumber()) continue;
            int n = i.getAsInt();
            if (n >= 2 && n <= 32) set.add(n);
        }
        return set;
    }

    static HudLayout.State parse(JsonObject root) {
        if (root == null || !root.has("e") || !root.get("e").isJsonObject()) throw new IllegalArgumentException("no elements");
        JsonElement v = root.get("v");
        if (v != null && (!v.isJsonPrimitive() || v.getAsInt() > 1)) throw new IllegalArgumentException("newer version");
        HudLayout.State s = new HudLayout.State();
        s.global.set(HudStyle.fromJson(root.get("g")));
        if (root.has("c") && root.get("c").isJsonObject()) {
            for (Map.Entry<String, JsonElement> en : root.getAsJsonObject("c").entrySet()) {
                TreeSet<Integer> set = readCopies(en.getValue());
                if (validId(en.getKey()) && !set.isEmpty()) s.copies.put(en.getKey(), set);
            }
        }
        int count = 0;
        for (Map.Entry<String, JsonElement> en : root.getAsJsonObject("e").entrySet()) {
            if (++count > 256) break;
            String id = en.getKey();
            if (!validId(id) || !en.getValue().isJsonObject()) continue;
            HudElement e = HudRegistry.ELEMENTS.get(id);
            if (e == null) e = copyFor(id, s);
            if (e == null) {
                s.unknown.put(id, en.getValue().getAsJsonObject());
                continue;
            }
            HudLayout.Placement p = new HudLayout.Placement(e);
            PlacementJson.read(e, id, p, en.getValue());
            s.placements.put(id, p);
        }
        return s;
    }

    private static HudElement copyFor(String id, HudLayout.State s) {
        int dot = id.lastIndexOf('.');
        if (dot <= 0) return null;
        HudElement base = HudRegistry.ELEMENTS.get(id.substring(0, dot));
        int n = copyIndexOf(id);
        if (base == null || !base.duplicable() || n < 2) return null;
        TreeSet<Integer> set = s.copies.get(base.id());
        return set != null && set.contains(n) ? base.copy(n) : null;
    }

    static int copyIndexOf(String id) {
        try {
            return Integer.parseInt(id.substring(id.lastIndexOf('.') + 1));
        } catch (RuntimeException e) {
            return -1;
        }
    }

    static boolean validId(String id) {
        return id != null && id.length() <= 64 && id.matches("[a-z0-9_.]+");
    }
}
