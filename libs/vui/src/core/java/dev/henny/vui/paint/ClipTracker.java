package dev.henny.vui.paint;

import dev.henny.vui.render.ScissorStack;

final class ClipTracker {
    private final ScissorStack stack = new ScissorStack();

    void reset(float width, float height) {
        while (stack.active()) stack.pop();
        stack.push(0, 0, width, height);
    }

    void push(float x, float y, float w, float h) {
        stack.push(x, y, w, h);
    }

    void pop() {
        if (stack.depth() > 1) stack.pop();
    }

    boolean intersects(float x, float y, float w, float h) {
        return x < stack.x2() && y < stack.y2() && x + w > stack.x1() && y + h > stack.y1();
    }
}
