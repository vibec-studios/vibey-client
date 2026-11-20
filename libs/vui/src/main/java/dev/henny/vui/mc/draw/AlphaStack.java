package dev.henny.vui.mc.draw;

final class AlphaStack {
    private float[] stack = new float[16];
    private int depth;
    private float value = 1f;

    void reset() {
        depth = 0;
        value = 1f;
    }

    float value() {
        return value;
    }

    void push(float alpha) {
        if (depth == stack.length) {
            float[] grown = new float[stack.length * 2];
            System.arraycopy(stack, 0, grown, 0, stack.length);
            stack = grown;
        }
        stack[depth++] = value;
        value *= alpha;
    }

    void pop() {
        if (depth > 0) value = stack[--depth];
    }
}
