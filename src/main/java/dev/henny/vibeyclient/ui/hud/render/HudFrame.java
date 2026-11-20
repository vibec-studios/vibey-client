package dev.henny.vibeyclient.ui.hud.render;

import dev.henny.vibeyclient.ui.Gfx;

final class HudFrame {
    private HudFrame() {}

    private static Gfx gfx;
    private static long now;
    private static boolean editor;

    static void begin(Gfx g, long nowMs, boolean editing) {
        gfx = g;
        now = nowMs;
        editor = editing;
    }

    static void end() {
        gfx = null;
    }

    static Gfx gfx() {
        return gfx;
    }

    static long now() {
        return now;
    }

    static boolean editor() {
        return editor;
    }
}
