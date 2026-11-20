package dev.henny.vibeyclient.client.names;

import dev.henny.vibeyclient.Compat;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
//? if >=1.21.9
import net.minecraft.network.chat.FontDescription;

public final class IconFont {
    private IconFont() {}

    public static final Style STYLE = font(Style.EMPTY).withColor(0xFFFFFF).withItalic(false).withBold(false)
            .withUnderlined(false).withStrikethrough(false).withObfuscated(false);

    public static MutableComponent glyph(char c, int color) {
        return Component.literal(String.valueOf(c)).setStyle(STYLE.withColor(color & 0xFFFFFF));
    }

    public static MutableComponent glyphs(String chars) {
        return Component.literal(chars).setStyle(STYLE);
    }

    private static Style font(Style style) {
        //? if >=1.21.9 {
        return style.withFont(new FontDescription.Resource(Compat.id("icons")));
        //?} else
        /*return style.withFont(Compat.id("icons"));*/
    }
}
