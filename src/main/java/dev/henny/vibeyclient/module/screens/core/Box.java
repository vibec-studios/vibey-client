package dev.henny.vibeyclient.module.screens.core;

public record Box(float x, float y, float w, float h) {
    public float right() {
        return x + w;
    }

    public float bottom() {
        return y + h;
    }

    public float cx() {
        return x + w / 2f;
    }

    public float cy() {
        return y + h / 2f;
    }

    public boolean contains(double px, double py) {
        return px >= x && py >= y && px < x + w && py < y + h;
    }

    public boolean containsCenterOf(Box o) {
        return contains(o.cx(), o.cy());
    }

    public Box at(float nx, float ny) {
        return new Box(nx, ny, w, h);
    }

    public Box shifted(float dx, float dy) {
        return new Box(x + dx, y + dy, w, h);
    }

    public Box sized(float nw, float nh) {
        return new Box(x, y, nw, nh);
    }

    public boolean same(Box o) {
        return o != null && Math.round(x) == Math.round(o.x) && Math.round(y) == Math.round(o.y)
                && Math.round(w) == Math.round(o.w) && Math.round(h) == Math.round(o.h);
    }
}
