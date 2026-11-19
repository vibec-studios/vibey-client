package dev.henny.vui.headless;

import dev.henny.vui.render.Canvas;

public final class NineSlice {
    private NineSlice() {
    }

    public static void draw(Canvas c, String id, float x, float y, float w, float h, int slice, int tw, int th, int tint) {
        float s = Math.min(slice, Math.min(w, h) / 2f);
        float us = slice / (float) tw;
        float vs = slice / (float) th;
        float[] xs = {x, x + s, x + w - s, x + w};
        float[] ys = {y, y + s, y + h - s, y + h};
        float[] us4 = {0, us, 1 - us, 1};
        float[] vs4 = {0, vs, 1 - vs, 1};
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                float cw = xs[col + 1] - xs[col];
                float ch = ys[row + 1] - ys[row];
                if (cw <= 0 || ch <= 0) continue;
                c.texture(id, xs[col], ys[row], cw, ch, us4[col], vs4[row], us4[col + 1], vs4[row + 1], tint);
            }
        }
    }
}
