package dev.henny.vibeyclient.module.servers.drag;

import net.minecraft.client.gui.GuiGraphics;

final class DragPaint {
    static final int ACCENT = 0xFF3FE0C5, GAP_FILL = 0x223FE0C5, SHADOW = 0x66000000, BODY = 0xFF101014;

    private DragPaint() {}

    static void gap(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, GAP_FILL);
        outline(g, x + 1, y + 1, w - 2, h - 2, 0x993FE0C5);
    }

    static void floating(GuiGraphics g, int x, int y, int w, int h) {
        g.nextStratum();
        g.fill(x + 3, y + 3, x + w + 3, y + h + 3, SHADOW);
        g.fill(x, y, x + w, y + h, BODY);
        outline(g, x, y, w, h, ACCENT);
    }

    private static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }
}
