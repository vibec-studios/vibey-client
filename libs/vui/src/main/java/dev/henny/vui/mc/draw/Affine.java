package dev.henny.vui.mc.draw;

final class Affine {
    private static final int SIZE = 6;
    private float[] stack = new float[SIZE * 16];
    private int depth;
    float a = 1, b, c, d = 1, tx, ty;

    void reset() {
        a = 1;
        b = 0;
        c = 0;
        d = 1;
        tx = 0;
        ty = 0;
        depth = 0;
    }

    int depth() {
        return depth;
    }

    void push() {
        if ((depth + 1) * SIZE > stack.length) {
            float[] grown = new float[stack.length * 2];
            System.arraycopy(stack, 0, grown, 0, stack.length);
            stack = grown;
        }
        int p = depth * SIZE;
        stack[p] = a;
        stack[p + 1] = b;
        stack[p + 2] = c;
        stack[p + 3] = d;
        stack[p + 4] = tx;
        stack[p + 5] = ty;
        depth++;
    }

    void pop() {
        if (depth == 0) return;
        depth--;
        int p = depth * SIZE;
        a = stack[p];
        b = stack[p + 1];
        c = stack[p + 2];
        d = stack[p + 3];
        tx = stack[p + 4];
        ty = stack[p + 5];
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
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        float na = a * cos + c * sin;
        float nb = b * cos + d * sin;
        float nc = -a * sin + c * cos;
        float nd = -b * sin + d * cos;
        a = na;
        b = nb;
        c = nc;
        d = nd;
    }

    boolean axisAligned() {
        return Math.abs(b) < 1e-6f && Math.abs(c) < 1e-6f;
    }

    boolean unitTranslation() {
        return axisAligned() && a == 1f && d == 1f && whole(tx) && whole(ty);
    }

    float x(float x, float y) {
        return a * x + c * y + tx;
    }

    float y(float x, float y) {
        return b * x + d * y + ty;
    }

    static boolean whole(float v) {
        return Math.abs(v - Math.round(v)) < 1e-3f;
    }
}
