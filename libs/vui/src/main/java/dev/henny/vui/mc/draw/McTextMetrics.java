package dev.henny.vui.mc.draw;

import dev.henny.vui.mc.compat.Mc;
import dev.henny.vui.render.TextMetrics;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.Arrays;

public final class McTextMetrics implements TextMetrics {
    private static final float FALLBACK_WIDTH = 6f;
    private static final float FALLBACK_LINE = 9f;

    private final float[][] plain = new float[256][];
    private final float[][] bold = new float[256][];

    @Override
    public float glyphWidth(int codepoint, boolean isBold) {
        if (!Mc.ready()) return FALLBACK_WIDTH;
        if (codepoint >= 0x10000) return measure(codepoint, isBold);
        float[][] pages = isBold ? bold : plain;
        float[] page = pages[codepoint >>> 8];
        if (page == null) {
            page = pages[codepoint >>> 8] = new float[256];
            Arrays.fill(page, Float.NaN);
        }
        float w = page[codepoint & 0xFF];
        if (Float.isNaN(w)) w = page[codepoint & 0xFF] = measure(codepoint, isBold);
        return w;
    }

    @Override
    public float lineHeight() {
        return Mc.ready() ? Mc.font().lineHeight : FALLBACK_LINE;
    }

    public void clear() {
        Arrays.fill(plain, null);
        Arrays.fill(bold, null);
    }

    private static float measure(int codepoint, boolean isBold) {
        Font font = Mc.font();
        String s = new String(Character.toChars(codepoint));
        if (!isBold) return font.width(s);
        return font.width(Component.literal(s).withStyle(ChatFormatting.BOLD));
    }
}
