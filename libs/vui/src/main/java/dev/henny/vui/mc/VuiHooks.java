package dev.henny.vui.mc;

import dev.henny.vui.mc.host.Guard;
import dev.henny.vui.mc.input.KeyCodes;
import dev.henny.vui.mc.input.MouseButtons;

public final class VuiHooks {
    private VuiHooks() {
    }

    public static void init(String version) {
        VuiMod.init(version);
        VuiRuntime.get();
    }

    public static void clientTick() {
        VuiRuntime.get().clientTick();
    }

    public static void hud(Object graphics) {
        Guard.run(null, () -> VuiRuntime.get().layers().onHud(graphics));
    }

    public static void afterScreen(Object graphics, int mouseX, int mouseY) {
        Guard.run(null, () -> VuiRuntime.get().layers().afterScreen(graphics, mouseX, mouseY));
    }

    public static boolean click(double x, double y, int nativeButton) {
        return Guard.test(null, () -> VuiRuntime.get().layers().click(x, y, MouseButtons.fromNative(nativeButton)));
    }

    public static boolean release(double x, double y, int nativeButton) {
        return Guard.test(null, () -> VuiRuntime.get().layers().release(x, y, MouseButtons.fromNative(nativeButton)));
    }

    public static boolean scroll(double x, double y, double dx, double dy) {
        return Guard.test(null, () -> VuiRuntime.get().layers().scroll(x, y, dx, dy));
    }

    public static boolean key(int nativeKey, int scancode) {
        return Guard.test(null, () -> VuiRuntime.get().layers().key(KeyCodes.fromNative(nativeKey), scancode));
    }
}
