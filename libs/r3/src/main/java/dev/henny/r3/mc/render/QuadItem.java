package dev.henny.r3.mc.render;

import dev.henny.r3.render.RenderMode;

import java.util.Arrays;
import java.util.Objects;

final class QuadItem {
    static final int FLOATS = 5;

    RenderMode mode;
    String texture;
    float[] xyzuv = new float[FLOATS * 64];
    int[] colorLight = new int[2 * 64];
    int vertices;

    QuadItem reset(RenderMode mode, String texture) {
        this.mode = mode;
        this.texture = texture;
        vertices = 0;
        return this;
    }

    boolean matches(RenderMode mode, String texture) {
        return this.mode == mode && Objects.equals(this.texture, texture);
    }

    void add(float x, float y, float z, float u, float v, int argb, int light) {
        if ((vertices + 1) * FLOATS > xyzuv.length) {
            xyzuv = Arrays.copyOf(xyzuv, xyzuv.length * 2);
            colorLight = Arrays.copyOf(colorLight, colorLight.length * 2);
        }
        int f = vertices * FLOATS;
        xyzuv[f] = x;
        xyzuv[f + 1] = y;
        xyzuv[f + 2] = z;
        xyzuv[f + 3] = u;
        xyzuv[f + 4] = v;
        colorLight[vertices * 2] = argb;
        colorLight[vertices * 2 + 1] = light;
        vertices++;
        if (vertices % 4 == 0) Winding.faceOrigin(this, vertices - 4);
    }

    void swap(int a, int b) {
        int fa = a * FLOATS, fb = b * FLOATS;
        for (int i = 0; i < FLOATS; i++) {
            float t = xyzuv[fa + i];
            xyzuv[fa + i] = xyzuv[fb + i];
            xyzuv[fb + i] = t;
        }
        for (int i = 0; i < 2; i++) {
            int t = colorLight[a * 2 + i];
            colorLight[a * 2 + i] = colorLight[b * 2 + i];
            colorLight[b * 2 + i] = t;
        }
    }

    float x(int v) {
        return xyzuv[v * FLOATS];
    }

    float y(int v) {
        return xyzuv[v * FLOATS + 1];
    }

    float z(int v) {
        return xyzuv[v * FLOATS + 2];
    }
}
