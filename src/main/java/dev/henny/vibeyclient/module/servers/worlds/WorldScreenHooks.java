package dev.henny.vibeyclient.module.servers.worlds;

import dev.henny.vibeyclient.module.servers.ServersModule;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;

import java.util.Map;
import java.util.WeakHashMap;

public final class WorldScreenHooks {
    private static final Map<SelectWorldScreen, WorldSession> OPEN = new WeakHashMap<>();
    private static boolean registered;

    private WorldScreenHooks() {}

    public static WorldSession current() {
        return Minecraft.getInstance().screen instanceof SelectWorldScreen s ? OPEN.get(s) : null;
    }

    public static void register(ServersModule m) {
        if (registered) return;
        registered = true;
        ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> {
            if (!(screen instanceof SelectWorldScreen s) || !m.isEnabled()) return;
            WorldSession session = OPEN.computeIfAbsent(s, k -> new WorldSession(m, k));
            ScreenEvents.afterRender(screen).register((sc, g, mx, my, d) -> session.render(g, mx, my, d));
            ScreenEvents.remove(screen).register(sc -> {
                WorldSession gone = OPEN.remove(s);
                if (gone != null) gone.close();
            });
            ScreenMouseEvents.allowMouseClick(screen).register((sc, e) -> session.click(e.x(), e.y(), e.button()));
            ScreenMouseEvents.allowMouseRelease(screen).register((sc, e) -> session.release(e.x(), e.y(), e.button()));
            ScreenKeyboardEvents.allowKeyPress(screen).register((sc, e) -> session.key(e.key()));
        });
    }
}
