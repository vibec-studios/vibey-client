package dev.henny.vibeyclient.module.hud.timer;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.screens.ChatScreen;

public final class ChatTimerInput {
    private ChatTimerInput() {}

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (!(screen instanceof ChatScreen)) return;
            ScreenMouseEvents.allowMouseScroll(screen).register((s, x, y, dx, dy) -> !scroll(s.width, s.height, x, y, dy));
            ScreenMouseEvents.allowMouseClick(screen).register((s, click) -> !click(s.width, s.height, click.x(), click.y()));
        });
    }

    private static boolean scroll(int sw, int sh, double x, double y, double dy) {
        TimerWidget w = Timers.at(x, y, sw, sh);
        return w != null && dy != 0 && w.wheel(w.element(), dy > 0 ? 1 : -1);
    }

    private static boolean click(int sw, int sh, double x, double y) {
        TimerWidget w = Timers.at(x, y, sw, sh);
        if (w == null) return false;
        w.toggle(System.currentTimeMillis());
        return true;
    }
}
