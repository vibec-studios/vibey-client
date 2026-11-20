package dev.henny.r3.mc.render;

import dev.henny.r3.mc.compat.Game;
import net.minecraft.client.gui.Font;

public final class McFontMetrics implements dev.henny.r3.render.TextMetrics, dev.henny.vui.render.TextMetrics {
    public static final McFontMetrics INSTANCE = new McFontMetrics();
    private static final int CACHED = 0x3000;

    private final float[] regular = new float[CACHED];
    private final float[] bold = new float[CACHED];
    private Font cachedFor;

    private McFontMetrics() {}

    @Override
    public float advance(int codepoint) {
        return glyphWidth(codepoint, false);
    }

    @Override
    public float glyphWidth(int codepoint, boolean isBold) {
        if (codepoint == '\n') return 0f;
        Font font = font();
        if (codepoint < 0 || codepoint >= CACHED) return measure(font, codepoint, isBold);
        float[] table = isBold ? bold : regular;
        float w = table[codepoint];
        if (w == 0f) {
            w = measure(font, codepoint, isBold) + 0.0001f;
            table[codepoint] = w;
        }
        return w - 0.0001f;
    }

    @Override
    public float lineHeight() {
        return font().lineHeight;
    }

    @Override
    public float width(String text) {
        return font().width(text);
    }

    private Font font() {
        Font font = Game.mc().font;
        if (font != cachedFor) {
            java.util.Arrays.fill(regular, 0f);
            java.util.Arrays.fill(bold, 0f);
            cachedFor = font;
        }
        return font;
    }

    private static float measure(Font font, int codepoint, boolean isBold) {
        String s = new String(Character.toChars(codepoint));
        return font.width(s) + (isBold ? 1f : 0f);
    }
}
