package dev.henny.vui.mc.draw;

final class Snap {
    float x1, y1, x2, y2;
    private float scale = 1;

    void scale(int guiScale) {
        scale = Math.max(1, guiScale);
    }

    float px(float v) {
        return Math.round(v * scale) / scale;
    }

    float minSize() {
        return 1f / scale;
    }

    boolean rect(Affine t, float x, float y, float w, float h) {
        float ax = px(t.x(x, y));
        float bx = px(t.x(x + w, y));
        float ay = px(t.y(x, y));
        float by = px(t.y(x, y + h));
        x1 = Math.min(ax, bx);
        x2 = Math.max(ax, bx);
        y1 = Math.min(ay, by);
        y2 = Math.max(ay, by);
        if (w > 0 && x2 <= x1) x2 = x1 + minSize();
        if (h > 0 && y2 <= y1) y2 = y1 + minSize();
        return x2 > x1 && y2 > y1;
    }

    boolean whole() {
        return Affine.whole(x1) && Affine.whole(y1) && Affine.whole(x2) && Affine.whole(y2);
    }
}
