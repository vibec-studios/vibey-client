package dev.henny.vui.icon;

import dev.henny.vui.render.Canvas;

public final class IconFills {
    private IconFills() {
    }

    public static void draw(Canvas canvas, PixelIcon icon, float x, float y, float w, float h, int argb) {
        if (icon == null || (argb >>> 24) == 0 || w <= 0 || h <= 0) return;
        float cell = cell(icon, w, h);
        float ox = x + (w - cell * icon.width()) / 2f;
        float oy = y + (h - cell * icon.height()) / 2f;
        if (cell >= 1) {
            ox = (float) Math.floor(ox);
            oy = (float) Math.floor(oy);
        }
        for (int i = 0; i < icon.runCount(); i++) {
            int color = icon.tinted() ? argb : fade(icon.runColor(i), argb >>> 24);
            canvas.fill(ox + icon.runX(i) * cell, oy + icon.runY(i) * cell, icon.runLength(i) * cell, cell, color);
        }
    }

    public static float cell(PixelIcon icon, float w, float h) {
        float fit = Math.min(w / icon.width(), h / icon.height());
        return fit >= 1 ? (float) Math.floor(fit) : fit;
    }

    static int fade(int color, int alpha) {
        if (alpha == 255) return color;
        int a = Math.round((color >>> 24) * alpha / 255f);
        return a << 24 | color & 0xFFFFFF;
    }
}
