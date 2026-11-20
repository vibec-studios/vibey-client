package dev.henny.r3.mc.render;

import dev.henny.r3.render.RenderMode;
import dev.henny.vui.render.ScissorStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

final class PanelQuads {
    private final Vector3f p = new Vector3f();
    private final float[] xs = new float[4];
    private final float[] ys = new float[4];
    private final float[] us = new float[4];
    private final float[] vs = new float[4];
    private final int[] colors = new int[4];

    void rect(DrawList list, Matrix4f world, Affine2D affine, ScissorStack scissors, float z, int light,
              String texture, float x, float y, float w, float h, float u0, float v0, float u1, float v1, int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        if (affine.axisAligned()) {
            float x0 = affine.x(x, y), y0 = affine.y(x, y);
            float x1 = affine.x(x + w, y + h), y1 = affine.y(x + w, y + h);
            if (x1 < x0) {
                float t = x0; x0 = x1; x1 = t;
                t = u0; u0 = u1; u1 = t;
            }
            if (y1 < y0) {
                float t = y0; y0 = y1; y1 = t;
                t = v0; v0 = v1; v1 = t;
                int c = top; top = bottom; bottom = c;
            }
            if (scissors.active()) {
                float cx0 = Math.max(x0, scissors.x1()), cy0 = Math.max(y0, scissors.y1());
                float cx1 = Math.min(x1, scissors.x2()), cy1 = Math.min(y1, scissors.y2());
                if (cx1 <= cx0 || cy1 <= cy0) return;
                float nu0 = lerp(u0, u1, (cx0 - x0) / (x1 - x0)), nu1 = lerp(u0, u1, (cx1 - x0) / (x1 - x0));
                float nv0 = lerp(v0, v1, (cy0 - y0) / (y1 - y0)), nv1 = lerp(v0, v1, (cy1 - y0) / (y1 - y0));
                int nTop = mix(top, bottom, (cy0 - y0) / (y1 - y0)), nBottom = mix(top, bottom, (cy1 - y0) / (y1 - y0));
                x0 = cx0; y0 = cy0; x1 = cx1; y1 = cy1;
                u0 = nu0; u1 = nu1; v0 = nv0; v1 = nv1; top = nTop; bottom = nBottom;
            }
            corners(x0, y0, x1, y1, u0, v0, u1, v1, top, bottom);
        } else {
            corners(x, y, x + w, y + h, u0, v0, u1, v1, top, bottom);
            for (int i = 0; i < 4; i++) {
                float px = xs[i], py = ys[i];
                xs[i] = affine.x(px, py);
                ys[i] = affine.y(px, py);
            }
        }
        QuadItem q = list.quads(RenderMode.TRANSLUCENT, texture);
        for (int i = 0; i < 4; i++) {
            world.transformPosition(xs[i], ys[i], z, p);
            q.add(p.x, p.y, p.z, us[i], vs[i], colors[i], light);
        }
    }

    void quad(DrawList list, Matrix4f world, Affine2D affine, float z, int light, float[] cornersXy, int argb) {
        QuadItem q = list.quads(RenderMode.TRANSLUCENT, null);
        for (int i = 0; i < 4; i++) {
            float px = affine.x(cornersXy[i * 2], cornersXy[i * 2 + 1]);
            float py = affine.y(cornersXy[i * 2], cornersXy[i * 2 + 1]);
            world.transformPosition(px, py, z, p);
            q.add(p.x, p.y, p.z, 0f, 0f, argb, light);
        }
    }

    private void corners(float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, int top, int bottom) {
        set(0, x0, y0, u0, v0, top);
        set(1, x0, y1, u0, v1, bottom);
        set(2, x1, y1, u1, v1, bottom);
        set(3, x1, y0, u1, v0, top);
    }

    private void set(int i, float x, float y, float u, float v, int color) {
        xs[i] = x;
        ys[i] = y;
        us[i] = u;
        vs[i] = v;
        colors[i] = color;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static int mix(int a, int b, float t) {
        return dev.henny.r3.math.Argb.lerp(a, b, t);
    }
}
