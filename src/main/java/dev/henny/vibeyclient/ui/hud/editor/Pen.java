package dev.henny.vibeyclient.ui.hud.editor;

import dev.henny.vui.render.Canvas;
import dev.henny.vui.render.TextMetrics;

final class Pen {
    private Canvas c;
    private TextMetrics m;

    Pen begin(Canvas canvas, TextMetrics metrics) {
        c = canvas;
        m = metrics;
        return this;
    }

    Canvas canvas() {
        return c;
    }

    void rect(float x, float y, float w, float h, int argb) {
        c.fill(x, y, w, h, argb);
    }

    void outline(float x, float y, float w, float h, int argb) {
        c.fill(x, y, w, 1, argb);
        c.fill(x, y + h - 1, w, 1, argb);
        c.fill(x, y + 1, 1, h - 2, argb);
        c.fill(x + w - 1, y + 1, 1, h - 2, argb);
    }

    void dashed(int x, int y, int w, int h, int argb) {
        for (int i = 0; i < w; i += 4) {
            int l = Math.min(2, w - i);
            c.fill(x + i, y, l, 1, argb);
            c.fill(x + i, y + h - 1, l, 1, argb);
        }
        for (int i = 0; i < h; i += 4) {
            int l = Math.min(2, h - i);
            c.fill(x, y + i, 1, l, argb);
            c.fill(x + w - 1, y + i, 1, l, argb);
        }
    }

    float width(String s) {
        return m.width(s);
    }

    void text(String s, float x, float y, int argb) {
        c.text(s, 0, s.length(), x, y, argb, false, false);
    }

    float chip(String s, float x, float y, int bg, int fg) {
        float w = width(s) + 8;
        c.fill(x, y, w, 11, bg);
        text(s, x + 4, y + 2, fg);
        return w;
    }
}
