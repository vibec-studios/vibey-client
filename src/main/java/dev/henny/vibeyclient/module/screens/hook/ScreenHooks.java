package dev.henny.vibeyclient.module.screens.hook;

import dev.henny.vibeyclient.module.screens.apply.ScreenSession;
import dev.henny.vibeyclient.module.screens.apply.Sessions;
import dev.henny.vibeyclient.module.screens.edit.EditMode;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.screens.Screen;

public final class ScreenHooks {
    private static boolean registered;

    private ScreenHooks() {}

    public static void register() {
        if (registered) return;
        registered = true;
        ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> {
            if (Sessions.usable(screen) == null) return;
            ScreenEvents.afterRender(screen).register((s, g, mx, my, d) -> ScreenInput.render(s, g, mx, my));
            ScreenEvents.remove(screen).register(EditMode::removed);
            ScreenMouseEvents.allowMouseClick(screen).register((s, e) -> !ScreenInput.click(s, e.x(), e.y(), e.button()));
            ScreenMouseEvents.allowMouseRelease(screen).register((s, e) -> !ScreenInput.release(s, e.x(), e.y()));
            ScreenMouseEvents.allowMouseDrag(screen).register((s, e, dx, dy) -> !ScreenInput.drag(s, e.x(), e.y()));
            ScreenMouseEvents.allowMouseScroll(screen).register((s, x, y, hx, vy) -> !ScreenInput.scroll(s, x, y, vy));
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, e) ->
                    !ScreenInput.key(s, e.key(), e.hasControlDown(), e.hasShiftDown()));
        });
    }

    static ScreenSession session(Screen screen) {
        return Sessions.usable(screen);
    }
}
