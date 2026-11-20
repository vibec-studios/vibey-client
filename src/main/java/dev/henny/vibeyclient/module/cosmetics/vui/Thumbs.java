package dev.henny.vibeyclient.module.cosmetics.vui;

import dev.henny.cosmetico.mc.render.Tex;
import dev.henny.cosmetico.mc.thumbs.McThumbnails;
import dev.henny.cosmetico.registry.Item;
import dev.henny.vui.mc.VuiMc;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class Thumbs {
    private static final Map<Tex, String> IDS = new HashMap<>();

    private Thumbs() {}

    static String of(Item item) {
        Tex t;
        try {
            t = McThumbnails.texture(item);
        } catch (RuntimeException e) {
            return "";
        }
        if (t == null || t.released()) return "";
        String known = IDS.get(t);
        if (known != null) return known;
        String id = "vibeyclient:thumb/" + item.id().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_") + "_" + IDS.size();
        VuiMc.textures().register(id, t.id, t.width, t.height);
        IDS.put(t, id);
        return id;
    }
}
