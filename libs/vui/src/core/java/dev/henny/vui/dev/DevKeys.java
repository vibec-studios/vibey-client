package dev.henny.vui.dev;

import com.google.gson.JsonObject;
import dev.henny.vui.dom.Document;
import dev.henny.vui.event.Keys;
import dev.henny.vui.event.Mods;

final class DevKeys {
    JsonObject send(Document doc, Query q) {
        int mods = mods(q.get("mods", ""));
        JsonObject o = new JsonObject();
        if (q.has("text")) {
            q.get("text").codePoints().forEach(cp -> doc.charTyped(cp, mods));
            o.addProperty("typed", q.get("text"));
        }
        if (q.has("key") || q.has("code")) {
            int key = q.has("code") ? q.number("code", 0) : Keys.byName(q.get("key").toLowerCase());
            boolean handled = doc.keyDown(key, 0, mods);
            doc.keyUp(key, 0, mods);
            o.addProperty("key", key);
            o.addProperty("handled", handled);
        }
        doc.frame(0);
        return o;
    }

    private static int mods(String list) {
        int m = 0;
        for (String part : list.split(",")) {
            switch (part.trim().toLowerCase()) {
                case "shift" -> m |= Mods.SHIFT;
                case "ctrl", "control" -> m |= Mods.CTRL;
                case "alt" -> m |= Mods.ALT;
                case "super", "meta", "cmd" -> m |= Mods.SUPER;
                default -> {
                }
            }
        }
        return m;
    }
}
