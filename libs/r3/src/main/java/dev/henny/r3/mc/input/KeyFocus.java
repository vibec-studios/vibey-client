package dev.henny.r3.mc.input;

import dev.henny.r3.input.InputCode;
import dev.henny.r3.mc.compat.Game;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.panel.KeyContent;
import dev.henny.r3.scene.R3Scene;

final class KeyFocus {
    private static final int ESCAPE = 256;
    private static final int FIRST_FUNCTION_KEY = 290;
    private static final int LAST_FUNCTION_KEY = 314;
    private static final int SHIFT = 1, CONTROL = 2, ALT = 4;
    private static final int LEFT_SHIFT = 340, LEFT_CONTROL = 341, LEFT_ALT = 342;

    private final HeldInput held;
    private final R3Scene scene;

    KeyFocus(HeldInput held, R3Scene scene) {
        this.held = held;
        this.scene = scene;
    }

    boolean active() {
        return Game.playing() && scene.keyTarget() != null;
    }

    boolean key(int glfwKey, int action) {
        WorldObject target = Game.playing() ? scene.keyTarget() : null;
        if (target == null) return false;
        KeyContent content = (KeyContent) target.content();
        int mods = mods();
        if (action == NativeInput.RELEASE) {
            content.keyUp(glfwKey, 0, mods);
            return true;
        }
        if (glfwKey == ESCAPE) {
            scene.releaseKeys();
            return true;
        }
        if (glfwKey >= FIRST_FUNCTION_KEY && glfwKey <= LAST_FUNCTION_KEY) return false;
        content.keyDown(glfwKey, 0, mods, action == NativeInput.REPEAT);
        return true;
    }

    boolean character(int codepoint) {
        WorldObject target = Game.playing() ? scene.keyTarget() : null;
        if (target == null) return false;
        ((KeyContent) target.content()).charTyped(codepoint, mods());
        return true;
    }

    void pointerPressed() {
        WorldObject target = scene.keyTarget();
        if (target != null && scene.interaction().hover().target() != target) scene.releaseKeys();
    }

    private int mods() {
        int m = 0;
        if (held.isDown(InputCode.key(LEFT_SHIFT))) m |= SHIFT;
        if (held.isDown(InputCode.key(LEFT_CONTROL))) m |= CONTROL;
        if (held.isDown(InputCode.key(LEFT_ALT))) m |= ALT;
        return m;
    }
}
