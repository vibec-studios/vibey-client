package dev.henny.r3.mc.input;

import dev.henny.r3.input.InputCode;
import dev.henny.r3.mc.compat.Game;
import dev.henny.r3.scene.R3Scene;

import java.util.BitSet;

public final class InputRouter {
    private final HeldInput held;
    private final R3Scene scene;
    private final BitSet consumedMouse = new BitSet(16);
    private final BitSet consumedKeys = new BitSet(512);

    public InputRouter(HeldInput held, R3Scene scene) {
        this.held = held;
        this.scene = scene;
    }

    public boolean mouseButton(int glfwButton, boolean down) {
        if (glfwButton < 0) return false;
        held.mouse(glfwButton, down);
        InputCode code = InputCode.mouse(glfwButton);
        if (down) {
            if (!Game.playing() || !scene.press(code)) return false;
            consumedMouse.set(glfwButton);
            return true;
        }
        scene.release(code);
        boolean consumed = consumedMouse.get(glfwButton);
        consumedMouse.clear(glfwButton);
        return consumed;
    }

    public boolean scroll(double notches) {
        if (!Game.playing() || notches == 0) return false;
        return scene.wheel(notches) || scene.blocksHotbarScroll();
    }

    public boolean key(int glfwKey, int action) {
        if (glfwKey < 0) return false;
        boolean down = action != NativeInput.RELEASE;
        held.key(glfwKey, down);
        if (action == NativeInput.REPEAT) return consumedKeys.get(glfwKey);
        InputCode code = InputCode.key(glfwKey);
        if (down) {
            if (!Game.playing() || !scene.press(code)) return false;
            consumedKeys.set(glfwKey);
            return true;
        }
        scene.release(code);
        boolean consumed = consumedKeys.get(glfwKey);
        consumedKeys.clear(glfwKey);
        return consumed;
    }

    public boolean nativeMouseButton(int nativeButton, int action) {
        if (action == NativeInput.REPEAT) return false;
        return mouseButton(NativeInput.mouseToGlfw(nativeButton), action == NativeInput.PRESS);
    }

    public boolean nativeKey(int nativeKey, int action) {
        int glfw = NativeInput.keyToGlfw(nativeKey);
        return key(KeyLayout.letter(glfw, nativeKey), action);
    }

    public void focusLost() {
        held.clear();
        consumedMouse.clear();
        consumedKeys.clear();
    }
}
