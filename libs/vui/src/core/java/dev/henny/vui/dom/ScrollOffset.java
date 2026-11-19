package dev.henny.vui.dom;

import dev.henny.vui.layout.LayoutBox;

final class ScrollOffset {
    private float x;
    private float y;

    float x() {
        return x;
    }

    float y() {
        return y;
    }

    void set(float nx, float ny) {
        x = nx;
        y = ny;
    }

    boolean clampTo(LayoutBox box, float nx, float ny) {
        float maxX = Math.max(0, box.contentWidth - box.innerWidth());
        float maxY = Math.max(0, box.contentHeight - box.innerHeight());
        float cx = Math.max(0, Math.min(maxX, nx));
        float cy = Math.max(0, Math.min(maxY, ny));
        if (cx == x && cy == y) return false;
        x = cx;
        y = cy;
        return true;
    }
}
