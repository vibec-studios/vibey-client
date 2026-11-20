package dev.henny.vui.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vui.layout.Anchor;

import java.util.Map;

public final class HudLayoutCodec {
    public static final int VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private HudLayoutCodec() {
    }

    public static String write(HudLayer layer) {
        JsonObject root = new JsonObject();
        root.addProperty("version", VERSION);
        JsonObject widgets = new JsonObject();
        for (HudWidget w : layer.widgets()) {
            HudPlacement p = w.placement();
            JsonObject o = new JsonObject();
            o.addProperty("anchor", p.anchor().cssName());
            o.addProperty("x", p.offsetX());
            o.addProperty("y", p.offsetY());
            o.addProperty("scale", p.scale());
            o.addProperty("visible", p.visible());
            widgets.add(w.id(), o);
        }
        root.add("widgets", widgets);
        return GSON.toJson(root);
    }

    public static int read(HudLayer layer, String json) {
        if (json == null || json.isBlank()) return 0;
        JsonElement parsed;
        try {
            parsed = JsonParser.parseString(json);
        } catch (RuntimeException e) {
            return 0;
        }
        if (!parsed.isJsonObject()) return 0;
        JsonElement widgets = parsed.getAsJsonObject().get("widgets");
        if (widgets == null || !widgets.isJsonObject()) return 0;
        int applied = 0;
        for (Map.Entry<String, JsonElement> e : widgets.getAsJsonObject().entrySet()) {
            HudWidget w = layer.widget(e.getKey());
            if (w == null || !e.getValue().isJsonObject()) continue;
            w.placement(placement(e.getValue().getAsJsonObject(), w.defaults()));
            applied++;
        }
        return applied;
    }

    private static HudPlacement placement(JsonObject o, HudPlacement fallback) {
        Anchor anchor = o.has("anchor") ? Anchor.fromCss(o.get("anchor").getAsString()) : null;
        if (anchor == null) anchor = fallback.anchor();
        float x = o.has("x") ? o.get("x").getAsFloat() : fallback.offsetX();
        float y = o.has("y") ? o.get("y").getAsFloat() : fallback.offsetY();
        float scale = o.has("scale") ? o.get("scale").getAsFloat() : fallback.scale();
        boolean visible = !o.has("visible") || o.get("visible").getAsBoolean();
        return new HudPlacement(anchor, x, y, 1f, visible).withScale(scale);
    }
}
