package dev.henny.vibeyclient.module.servers.screen;

import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.servers.ServersModule;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;

public final class ServerScreenHooks {
    private static boolean registered;

    private ServerScreenHooks() {}

    public static void register(ServersModule m) {
        if (registered) return;
        registered = true;
        ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> {
            if (!(screen instanceof JoinMultiplayerScreen s) || !m.isEnabled()) return;
            ServerSession session = ServerSessions.open(m, s);
            ScreenEvents.afterRender(screen).register((sc, g, mx, my, d) -> session.render(g, mx, my, d));
            ScreenEvents.afterTick(screen).register(sc -> session.tick());
            ScreenEvents.remove(screen).register(sc -> ServerSessions.close(s));
            ScreenMouseEvents.allowMouseClick(screen).register((sc, e) -> session.click(e.x(), e.y(), e.button()));
            ScreenMouseEvents.allowMouseRelease(screen).register((sc, e) -> session.release(e.x(), e.y(), e.button()));
            ScreenMouseEvents.allowMouseScroll(screen).register((sc, x, y, hx, vy) -> session.scroll(x, y, hx, vy));
            ScreenKeyboardEvents.allowKeyPress(screen).register((sc, e) -> session.key(e.key(), e.scancode(), e.hasControlDown() || ClientCompat.controlDown()));
        });
    }
}
