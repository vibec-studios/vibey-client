package dev.henny.vibeyclient.ui.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Locale;
import java.util.Map;

final class PlacementJson {
    private PlacementJson() {}

    static JsonObject write(HudLayout.Placement p) {
        JsonObject o = new JsonObject();
        o.addProperty("a", p.anchor.name().toLowerCase(Locale.ROOT));
        if (!p.autoAnchor) o.addProperty("fix", true);
        o.addProperty("x", p.dx);
        o.addProperty("y", p.dy);
        o.addProperty("s", Math.round(p.scale * 1000f) / 1000f);
        if (p.bw > 0) o.addProperty("w", p.bw);
        if (p.bh > 0) o.addProperty("h", p.bh);
        if (p.hidden) o.addProperty("hide", true);
        if (!p.style.isEmpty()) o.add("st", p.style.toJson());
        if (!p.opts.isEmpty()) {
            JsonObject op = new JsonObject();
            for (Map.Entry<String, String> en : p.opts.entrySet()) op.addProperty(en.getKey(), en.getValue());
            o.add("o", op);
        }
        return o;
    }

    static void read(String id, HudLayout.Placement p, JsonElement el) {
        read(HudRegistry.ELEMENTS.get(id), id, p, el);
    }

    static void read(HudElement e, String id, HudLayout.Placement p, JsonElement el) {
        if (el == null || !el.isJsonObject()) return;
        JsonObject o = el.getAsJsonObject();
        try {
            Anchor a = JsonRead.enumOrNull(Anchor.class, o, o.has("a") ? "a" : "anchor");
            if (a != null) p.anchor = a;
            p.autoAnchor = !JsonRead.bool(o, "fix", false);
            p.dx = JsonRead.clamp(JsonRead.num(o, "x", p.dx), -4096, 4096);
            p.dy = JsonRead.clamp(JsonRead.num(o, "y", p.dy), -4096, 4096);
            float s = (float) JsonRead.numF(o, "s", JsonRead.numF(o, "scale", p.scale));
            p.scale = Float.isFinite(s) ? s : p.scale;
            p.bw = JsonRead.clamp(JsonRead.num(o, "w", 0), 0, 2048);
            p.bh = JsonRead.clamp(JsonRead.num(o, "h", 0), 0, 2048);
            p.hidden = JsonRead.bool(o, "hide", JsonRead.bool(o, "hidden", false));
            p.style.set(HudStyle.fromJson(o.get("st")));
            readOptions(e, p, o);
            p.custom = true;
            p.scale = e != null ? HudGeometry.clampScale(e, p.scale) : Math.max(0.25f, Math.min(4f, p.scale));
        } catch (RuntimeException ex) {
            dev.henny.vibeyclient.Vibey.LOG.warn("Bad HUD placement for {}: {}", id, o);
        }
        HudCaches.bumpStyle();
    }

    private static void readOptions(HudElement e, HudLayout.Placement p, JsonObject o) {
        p.opts.clear();
        if (!o.has("o") || !o.get("o").isJsonObject()) return;
        for (Map.Entry<String, JsonElement> en : o.getAsJsonObject("o").entrySet()) {
            if (!en.getValue().isJsonPrimitive()) continue;
            String v = en.getValue().getAsString();
            HudOption opt = e == null ? null : HudEdits.option(e, en.getKey());
            if (e == null || (opt != null && opt.accepts(v))) p.opts.put(en.getKey(), v);
        }
    }
}
