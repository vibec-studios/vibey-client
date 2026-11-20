package dev.henny.vui.mc.draw;

import dev.henny.vui.render.Canvas;

final class NineSlices {
    private NineSlices() {
    }

    static void draw(Canvas c, String id, float x, float y, float w, float h, int slice, int tw, int th, int tint) {
        if (tw <= 0 || th <= 0) return;
        float s = Math.min(slice, Math.min(w, h) / 2f);
        float us = slice / (float) tw;
        float vs = slice / (float) th;
        for (int row = 0; row < 3; row++) {
            float y0 = edge(row, y, h, s);
            float y1 = edge(row + 1, y, h, s);
            float v0 = edge(row, 0, 1, vs);
            float v1 = edge(row + 1, 0, 1, vs);
            for (int col = 0; col < 3; col++) {
                float x0 = edge(col, x, w, s);
                float x1 = edge(col + 1, x, w, s);
                if (x1 - x0 <= 0 || y1 - y0 <= 0) continue;
                c.texture(id, x0, y0, x1 - x0, y1 - y0, edge(col, 0, 1, us), v0, edge(col + 1, 0, 1, us), v1, tint);
            }
        }
    }

    private static float edge(int index, float start, float size, float inset) {
        return switch (index) {
            case 0 -> start;
            case 1 -> start + inset;
            case 2 -> start + size - inset;
            default -> start + size;
        };
    }
}
