package dev.henny.vui.mc.neoforge;

import dev.henny.vui.mc.VuiHooks;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

final class NeoScreens {
    private NeoScreens() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Render.Post e) -> VuiHooks.afterScreen(e.getGuiGraphics(), e.getMouseX(), e.getMouseY()));
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseButtonPressed.Pre e) -> {
            if (VuiHooks.click(e.getMouseX(), e.getMouseY(), e.getButton())) e.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseButtonReleased.Pre e) -> {
            if (VuiHooks.release(e.getMouseX(), e.getMouseY(), e.getButton())) e.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseScrolled.Pre e) -> {
            if (VuiHooks.scroll(e.getMouseX(), e.getMouseY(), e.getScrollDeltaX(), e.getScrollDeltaY())) e.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.KeyPressed.Pre e) -> {
            if (VuiHooks.key(key(e), scancode(e))) e.setCanceled(true);
        });
    }

    private static int key(ScreenEvent.KeyPressed e) {
        //? if >=26.3 {
        /*return e.getKey();
        *///?} else
        return e.getKeyCode();
    }

    private static int scancode(ScreenEvent.KeyPressed e) {
        //? if >=26.3 {
        /*return 0;
        *///?} else
        return e.getScanCode();
    }
}
