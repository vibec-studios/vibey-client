package dev.henny.vui.headless;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

final class Placeholder {
    private static final Color MAGENTA = new Color(0xFFFF00FF, true);
    private static final Color BLACK = new Color(0xFF000000, true);

    private Placeholder() {
    }

    static void draw(Graphics2D g, float x, float y, float w, float h) {
        float cell = Math.max(2, Math.min(w, h) / 2f);
        for (float cy = 0; cy < h; cy += cell) {
            for (float cx = 0; cx < w; cx += cell) {
                boolean odd = ((int) (cx / cell) + (int) (cy / cell)) % 2 == 1;
                g.setColor(odd ? BLACK : MAGENTA);
                g.fill(new Rectangle2D.Float(x + cx, y + cy, Math.min(cell, w - cx), Math.min(cell, h - cy)));
            }
        }
    }
}
