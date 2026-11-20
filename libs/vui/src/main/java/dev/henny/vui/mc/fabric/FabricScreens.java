package dev.henny.vui.mc.fabric;

import dev.henny.vui.mc.VuiHooks;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;

final class FabricScreens {
    private FabricScreens() {
    }

    static void register() {
        ScreenEvents.AFTER_INIT.register((mc, screen, width, height) -> {
            //? if >=26.1 {
            /*ScreenEvents.afterExtract(screen).register((s, g, mx, my, delta) -> VuiHooks.afterScreen(g, mx, my));
            *///?} else
            ScreenEvents.afterRender(screen).register((s, g, mx, my, delta) -> VuiHooks.afterScreen(g, mx, my));
            //? if >=1.21.9 {
            ScreenMouseEvents.allowMouseClick(screen).register((s, e) -> !VuiHooks.click(e.x(), e.y(), e.button()));
            ScreenMouseEvents.allowMouseRelease(screen).register((s, e) -> !VuiHooks.release(e.x(), e.y(), e.button()));
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, e) -> !VuiHooks.key(e.key(), 0));
            //?} else {
            /*ScreenMouseEvents.allowMouseClick(screen).register((s, x, y, button) -> !VuiHooks.click(x, y, button));
            ScreenMouseEvents.allowMouseRelease(screen).register((s, x, y, button) -> !VuiHooks.release(x, y, button));
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, key, scancode, mods) -> !VuiHooks.key(key, scancode));
            *///?}
            ScreenMouseEvents.allowMouseScroll(screen).register((s, x, y, dx, dy) -> !VuiHooks.scroll(x, y, dx, dy));
        });
    }
}
