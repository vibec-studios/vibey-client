package dev.henny.vibeyclient.module.inventory.screen;

import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.inventory.InventoryModule;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;

public final class ScreenHooks {
    private static boolean registered;

    private ScreenHooks() {}

    public static void register(InventoryModule m) {
        if (registered) return;
        registered = true;
        ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> {
            if (!(screen instanceof AbstractContainerScreen<?> s) || screen instanceof CreativeModeInventoryScreen) return;
            ContainerSession session = ContainerSessions.open(m, s);
            ScreenEvents.afterRender(screen).register((sc, g, mx, my, d) -> session.render(g, mx, my));
            ScreenEvents.remove(screen).register(sc -> ContainerSessions.close(s));
            ScreenMouseEvents.allowMouseClick(screen).register((sc, e) -> session.click(e.x(), e.y(), e.button(), e.hasShiftDown() || ClientCompat.shiftDown()));
            ScreenMouseEvents.allowMouseRelease(screen).register((sc, e) -> session.release(e.x(), e.y(), e.button()));
            ScreenMouseEvents.allowMouseScroll(screen).register((sc, x, y, dx, dy) -> session.scroll(x, y, dy));
            ScreenKeyboardEvents.allowKeyPress(screen).register((sc, e) -> session.key(e.key(), e.scancode(), e.hasControlDown() || ClientCompat.controlDown()));
        });
    }
}
