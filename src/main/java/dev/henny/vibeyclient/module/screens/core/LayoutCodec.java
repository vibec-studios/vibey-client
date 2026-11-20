package dev.henny.vibeyclient.module.screens.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.LinkedHashMap;
import java.util.Map;

public final class LayoutCodec {
    public static final int VERSION = 1;

    private LayoutCodec() {}

    public static JsonObject write(Map<String, ScreenLayout> screens) {
        JsonObject root = new JsonObject();
        root.addProperty("version", VERSION);
        JsonObject all = new JsonObject();
        screens.forEach((name, l) -> {
            if (!l.isEmpty()) all.add(name, screen(l));
        });
        root.add("screens", all);
        return root;
    }

    public static Map<String, ScreenLayout> read(String json) {
        Map<String, ScreenLayout> out = new LinkedHashMap<>();
        if (json == null || json.isBlank()) return out;
        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (RuntimeException e) {
            return out;
        }
        if (!root.isJsonObject() || !(root.getAsJsonObject().get("screens") instanceof JsonObject all)) return out;
        for (Map.Entry<String, JsonElement> e : all.entrySet()) {
            if (!(e.getValue() instanceof JsonObject o)) continue;
            ScreenLayout l = screen(o);
            if (!l.isEmpty()) out.put(e.getKey(), l);
        }
        return out;
    }

    static JsonObject screen(ScreenLayout l) {
        JsonObject o = new JsonObject();
        if (l.scale() != 1f) o.addProperty("scale", l.scale());
        if (l.dim() != ScreenLayout.VANILLA) o.addProperty("dim", l.dim());
        if (l.blur() != ScreenLayout.VANILLA) o.addProperty("blur", l.blur());
        if (l.hideTitle()) o.addProperty("hideTitle", true);
        if (l.label() != null) o.addProperty("label", l.label());
        JsonObject w = new JsonObject();
        l.widgets().forEach((id, p) -> w.add(id, placement(p)));
        if (!w.isEmpty()) o.add("widgets", w);
        return o;
    }

    static ScreenLayout screen(JsonObject o) {
        Map<String, Placement> widgets = new LinkedHashMap<>();
        if (o.get("widgets") instanceof JsonObject w) {
            for (Map.Entry<String, JsonElement> e : w.entrySet()) {
                if (e.getValue() instanceof JsonObject p) widgets.put(e.getKey(), placement(p));
            }
        }
        return new ScreenLayout(num(o, "scale", 1f), (int) num(o, "dim", -1), (int) num(o, "blur", -1),
                bool(o, "hideTitle"), widgets, str(o, "label"));
    }

    static JsonObject placement(Placement p) {
        JsonObject o = new JsonObject();
        if (p.pos() != null) {
            o.addProperty("ax", p.pos().ax());
            o.addProperty("ay", p.pos().ay());
            o.addProperty("x", p.pos().ox());
            o.addProperty("y", p.pos().oy());
        }
        if (p.scale() != 1f) o.addProperty("scale", p.scale());
        if (p.width() > 0) o.addProperty("width", p.width());
        if (p.hidden()) o.addProperty("hidden", true);
        if (p.fallback() != null) o.addProperty("fallback", p.fallback());
        return o;
    }

    static Placement placement(JsonObject o) {
        Pos pos = null;
        if (o.has("ax") && o.has("ay")) {
            pos = new Pos(clampAnchor((int) num(o, "ax", 1)), clampAnchor((int) num(o, "ay", 1)), num(o, "x", 0), num(o, "y", 0));
        }
        return new Placement(pos, num(o, "scale", 1f), (int) num(o, "width", 0), bool(o, "hidden"), str(o, "fallback"));
    }

    private static int clampAnchor(int a) {
        return Math.max(0, Math.min(2, a));
    }

    private static float num(JsonObject o, String key, float def) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber()) return def;
        float v = e.getAsFloat();
        return Float.isFinite(v) ? v : def;
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString() ? e.getAsString() : null;
    }

    private static boolean bool(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean() && e.getAsBoolean();
    }
}
