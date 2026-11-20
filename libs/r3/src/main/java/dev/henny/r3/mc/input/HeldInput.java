package dev.henny.r3.mc.input;

import dev.henny.r3.input.HeldKeys;
import dev.henny.r3.input.InputCode;

import java.util.BitSet;

public final class HeldInput implements HeldKeys {
    private static final int LEFT_SHIFT = 340, RIGHT_SHIFT = 344;
    private static final int LEFT_CONTROL = 341, RIGHT_CONTROL = 345;
    private static final int LEFT_ALT = 342, RIGHT_ALT = 346;

    private final BitSet keys = new BitSet(512);
    private final BitSet mouse = new BitSet(16);

    public void key(int glfw, boolean down) {
        if (glfw >= 0 && glfw < 1024) keys.set(glfw, down);
    }

    public void mouse(int button, boolean down) {
        if (button >= 0 && button < 16) mouse.set(button, down);
    }

    public void clear() {
        keys.clear();
        mouse.clear();
    }

    @Override
    public boolean isDown(InputCode code) {
        return switch (code.device()) {
            case MOUSE -> mouse.get(code.code());
            case KEY -> key(code.code());
            default -> false;
        };
    }

    private boolean key(int glfw) {
        if (glfw == LEFT_SHIFT || glfw == RIGHT_SHIFT) return keys.get(LEFT_SHIFT) || keys.get(RIGHT_SHIFT);
        if (glfw == LEFT_CONTROL || glfw == RIGHT_CONTROL) return keys.get(LEFT_CONTROL) || keys.get(RIGHT_CONTROL);
        if (glfw == LEFT_ALT || glfw == RIGHT_ALT) return keys.get(LEFT_ALT) || keys.get(RIGHT_ALT);
        return glfw >= 0 && keys.get(glfw);
    }
}
