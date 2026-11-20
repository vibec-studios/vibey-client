package dev.henny.r3.mc.render;

import org.joml.Matrix4f;

import java.util.Arrays;

final class Affine2D {
    private static final int SIZE = 6;
    private float[] stack = new float[SIZE * 16];
    private int depth;
    private float a = 1, b, c, d = 1, tx, ty;

    void reset() {
        depth = 0;
        a = 1;
        b = 0;
        c = 0;
        d = 1;
        tx = 0;
        ty = 0;
    }

    void push() {
        if ((depth + 1) * SIZE > stack.length) stack = Arrays.copyOf(stack, stack.length * 2);
        int i = depth * SIZE;
        stack[i] = a;
        stack[i + 1] = b;
        stack[i + 2] = c;
        stack[i + 3] = d;
        stack[i + 4] = tx;
        stack[i + 5] = ty;
        depth++;
    }

    void pop() {
        if (depth == 0) return;
        depth--;
        int i = depth * SIZE;
        a = stack[i];
        b = stack[i + 1];
        c = stack[i + 2];
        d = stack[i + 3];
        tx = stack[i + 4];
        ty = stack[i + 5];
    }

    void translate(float x, float y) {
        tx += a * x + c * y;
        ty += b * x + d * y;
    }

    void scale(float sx, float sy) {
        a *= sx;
        b *= sx;
        c *= sy;
        d *= sy;
    }

    void rotate(float radians) {
        float cos = (float) Math.cos(radians), sin = (float) Math.sin(radians);
        float na = a * cos + c * sin, nb = b * cos + d * sin;
        float nc = c * cos - a * sin, nd = d * cos - b * sin;
        a = na;
        b = nb;
        c = nc;
        d = nd;
    }

    boolean axisAligned() {
        return b == 0f && c == 0f;
    }

    float x(float x, float y) {
        return a * x + c * y + tx;
    }

    float y(float x, float y) {
        return b * x + d * y + ty;
    }

    Matrix4f toMatrix(float z, Matrix4f out) {
        return out.set(a, b, 0, 0, c, d, 0, 0, 0, 0, 1, 0, tx, ty, z, 1);
    }
}
