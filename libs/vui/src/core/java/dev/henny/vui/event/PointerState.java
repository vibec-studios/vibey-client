package dev.henny.vui.event;

import dev.henny.vui.dom.Element;

public final class PointerState {
    public static final float DRAG_THRESHOLD = 3f;
    public static final float DOUBLE_CLICK_SECONDS = 0.3f;

    Element hovered;
    Element pressed;
    int button = -1;
    float pressX;
    float pressY;
    float lastX;
    float lastY;
    boolean dragging;
    Element lastClickTarget;
    float lastClickTime = -10f;

    void press(Element target, int button, float x, float y) {
        pressed = target;
        this.button = button;
        pressX = x;
        pressY = y;
        dragging = false;
    }

    void release() {
        pressed = null;
        button = -1;
        dragging = false;
    }

    void forget(Element e) {
        if (hovered == e) hovered = null;
        if (pressed == e) release();
        if (lastClickTarget == e) lastClickTarget = null;
    }

    public Element pressed() {
        return pressed;
    }

    public boolean dragging() {
        return dragging;
    }

    public float x() {
        return lastX;
    }

    public float y() {
        return lastY;
    }
}
