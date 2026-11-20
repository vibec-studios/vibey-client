package dev.henny.vibeyclient.module.screens.edit;

import dev.henny.vibeyclient.module.screens.core.Box;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

final class Draw {
    static final int CHIP_H = 13;
    static final int CHIP_BG = 0xE60B0B0E;

    private Draw() {}

    static Font font() {
        return Minecraft.getInstance().font;
    }

    static void rect(GuiGraphics g, float x, float y, float w, float h, int argb) {
        int x0 = Math.round(x), y0 = Math.round(y);
        g.fill(x0, y0, x0 + Math.max(1, Math.round(w)), y0 + Math.max(1, Math.round(h)), argb);
    }

    static void outline(GuiGraphics g, Box b, int argb) {
        int x = Math.round(b.x()) - 1, y = Math.round(b.y()) - 1, w = Math.round(b.w()) + 2, h = Math.round(b.h()) + 2;
        g.fill(x, y, x + w, y + 1, argb);
        g.fill(x, y + h - 1, x + w, y + h, argb);
        g.fill(x, y + 1, x + 1, y + h - 1, argb);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, argb);
    }

    static void dashed(GuiGraphics g, Box b, int argb) {
        int x = Math.round(b.x()) - 1, y = Math.round(b.y()) - 1, w = Math.round(b.w()) + 2, h = Math.round(b.h()) + 2;
        for (int i = 0; i < w; i += 4) {
            int l = Math.min(2, w - i);
            g.fill(x + i, y, x + i + l, y + 1, argb);
            g.fill(x + i, y + h - 1, x + i + l, y + h, argb);
        }
        for (int i = 0; i < h; i += 4) {
            int l = Math.min(2, h - i);
            g.fill(x, y + i, x + 1, y + i + l, argb);
            g.fill(x + w - 1, y + i, x + w, y + i + l, argb);
        }
    }

    static int width(String s) {
        return font().width(s);
    }

    static void text(GuiGraphics g, String s, float x, float y, int argb) {
        g.drawString(font(), s, Math.round(x), Math.round(y), argb, false);
    }

    static int chip(GuiGraphics g, String s, float x, float y, int bg, int fg) {
        int w = width(s) + 8;
        rect(g, x, y, w, CHIP_H - 2, bg);
        text(g, s, x + 4, y + 2, fg);
        return w;
    }

    static void pointer(GuiGraphics g, double mx, double my) {
        int x = (int) Math.round(mx), y = (int) Math.round(my);
        for (int i = 0; i < 9; i++) {
            g.fill(x, y + i, x + Math.max(1, i * 2 / 3), y + i + 1, 0xFF000000);
            if (i > 1 && i < 8) g.fill(x + 1, y + i, x + Math.max(1, i * 2 / 3) - 1 + 1, y + i + 1, 0xFFFFFFFF);
        }
    }
}
