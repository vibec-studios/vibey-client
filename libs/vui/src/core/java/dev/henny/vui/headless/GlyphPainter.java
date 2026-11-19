package dev.henny.vui.headless;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

final class GlyphPainter {
    private GlyphPainter() {
    }

    static void draw(Graphics2D g, int codepoint, float x, float y, Color color, boolean bold) {
        g.setColor(color);
        int[] rows = PixelFont.glyph(codepoint);
        if (rows == null) {
            box(g, x, y);
            return;
        }
        for (int row = 0; row < PixelFont.HEIGHT; row++) {
            for (int col = 0; col < PixelFont.WIDTH; col++) {
                if (!PixelFont.pixel(rows, col, row)) continue;
                g.fill(new Rectangle2D.Float(x + col, y + row, bold ? 2 : 1, 1));
            }
        }
    }

    private static void box(Graphics2D g, float x, float y) {
        g.fill(new Rectangle2D.Float(x, y, PixelFont.WIDTH, 1));
        g.fill(new Rectangle2D.Float(x, y + PixelFont.HEIGHT - 1, PixelFont.WIDTH, 1));
        g.fill(new Rectangle2D.Float(x, y, 1, PixelFont.HEIGHT));
        g.fill(new Rectangle2D.Float(x + PixelFont.WIDTH - 1, y, 1, PixelFont.HEIGHT));
    }
}
