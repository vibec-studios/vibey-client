package dev.henny.vui.event;

import dev.henny.vui.dom.Element;

public final class PointerEvent extends Event {
    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int MIDDLE = 2;

    private float x;
    private float y;
    private float dx;
    private float dy;
    private int button;
    private int clicks;
    private int mods;

    public PointerEvent pointer(String type, Element target, float x, float y, int button, int mods) {
        reset(type, target, true, null);
        this.x = x;
        this.y = y;
        this.button = button;
        this.mods = mods;
        this.dx = 0;
        this.dy = 0;
        this.clicks = 0;
        return this;
    }

    public PointerEvent delta(float dx, float dy) {
        this.dx = dx;
        this.dy = dy;
        return this;
    }

    public PointerEvent clicks(int count) {
        this.clicks = count;
        return this;
    }

    public PointerEvent bubbling(boolean bubbles) {
        reset(type(), target(), bubbles, null);
        return this;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float localX() {
        return x - currentTarget().box().x;
    }

    public float localY() {
        return y - currentTarget().box().y;
    }

    public float dx() {
        return dx;
    }

    public float dy() {
        return dy;
    }

    public int button() {
        return button;
    }

    public int clicks() {
        return clicks;
    }

    public int mods() {
        return mods;
    }

    public boolean shift() {
        return (mods & Mods.SHIFT) != 0;
    }

    public boolean ctrl() {
        return (mods & Mods.CTRL) != 0;
    }

    public boolean alt() {
        return (mods & Mods.ALT) != 0;
    }
}
