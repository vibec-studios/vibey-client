package dev.henny.r3.mc.render;

import dev.henny.r3.mc.compat.Ids;
import net.minecraft.network.chat.Style;
//? if >=1.21.9 {
import net.minecraft.network.chat.FontDescription;
//?}

import java.util.HashMap;
import java.util.Map;

final class FontStyles {
    private static final Style BOLD = Style.EMPTY.withBold(true);
    private static final Map<String, Style> CACHE = new HashMap<>();

    private FontStyles() {}

    static Style of(String font, boolean bold) {
        if (font == null || font.isEmpty() || "default".equals(font)) return bold ? BOLD : Style.EMPTY;
        String key = font + (bold ? "|b" : "");
        Style known = CACHE.get(key);
        if (known != null) return known;
        Style style = withFont(Style.EMPTY.withBold(bold), font);
        CACHE.put(key, style);
        return style;
    }

    private static Style withFont(Style style, String font) {
        var id = Ids.parse(font);
        if (id == null) return style;
        //? if >=1.21.9 {
        return style.withFont(new FontDescription.Resource(id));
        //?} else
        /*return style.withFont(id);*/
    }
}
