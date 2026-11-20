package dev.henny.r3.vui;

import dev.henny.r3.render.PanelCanvas;
import dev.henny.r3.render.TextureHandle;

final class NineSlice {
    private final float[] xs = new float[4];
    private final float[] ys = new float[4];
    private final float[] us = new float[4];
    private final float[] vs = new float[4];

    void draw(PanelCanvas canvas, TextureHandle texture, float x, float y, float w, float h, int slice, int texW, int texH, int tint) {
        float sx = Math.min(slice, w / 2f);
        float sy = Math.min(slice, h / 2f);
        split(xs, x, w, sx);
        split(ys, y, h, sy);
        uv(us, slice / (float) Math.max(1, texW));
        uv(vs, slice / (float) Math.max(1, texH));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                float cw = xs[col + 1] - xs[col];
                float ch = ys[row + 1] - ys[row];
                if (cw <= 0 || ch <= 0) continue;
                canvas.texture(texture, xs[col], ys[row], cw, ch, us[col], vs[row], us[col + 1], vs[row + 1], tint);
            }
        }
    }

    private static void split(float[] out, float start, float size, float edge) {
        out[0] = start;
        out[1] = start + edge;
        out[2] = start + size - edge;
        out[3] = start + size;
    }

    private static void uv(float[] out, float edge) {
        out[0] = 0f;
        out[1] = edge;
        out[2] = 1f - edge;
        out[3] = 1f;
    }
}
