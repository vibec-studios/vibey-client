package dev.henny.vui.render;

public final class ScissorStack {
    private float[] rects = new float[4 * 16];
    private int depth;

    public void push(float x, float y, float w, float h) {
        float x1 = x;
        float y1 = y;
        float x2 = x + w;
        float y2 = y + h;
        if (depth > 0) {
            int p = (depth - 1) * 4;
            x1 = Math.max(x1, rects[p]);
            y1 = Math.max(y1, rects[p + 1]);
            x2 = Math.min(x2, rects[p + 2]);
            y2 = Math.min(y2, rects[p + 3]);
        }
        if (x2 < x1) x2 = x1;
        if (y2 < y1) y2 = y1;
        if ((depth + 1) * 4 > rects.length) {
            float[] grown = new float[rects.length * 2];
            System.arraycopy(rects, 0, grown, 0, rects.length);
            rects = grown;
        }
        int q = depth * 4;
        rects[q] = x1;
        rects[q + 1] = y1;
        rects[q + 2] = x2;
        rects[q + 3] = y2;
        depth++;
    }

    public void pop() {
        if (depth > 0) depth--;
    }

    public boolean active() {
        return depth > 0;
    }

    public int depth() {
        return depth;
    }

    public float x1() {
        return rects[(depth - 1) * 4];
    }

    public float y1() {
        return rects[(depth - 1) * 4 + 1];
    }

    public float x2() {
        return rects[(depth - 1) * 4 + 2];
    }

    public float y2() {
        return rects[(depth - 1) * 4 + 3];
    }

    public boolean contains(float x, float y) {
        return depth == 0 || (x >= x1() && x < x2() && y >= y1() && y < y2());
    }
}
