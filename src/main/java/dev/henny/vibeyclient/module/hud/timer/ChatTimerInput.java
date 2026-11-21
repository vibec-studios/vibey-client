package dev.henny.vibeyclient.module.hud.timer;

import dev.henny.vibeyclient.client.ScreenInput;
import net.minecraft.client.gui.screens.ChatScreen;

public final class ChatTimerInput {
    private ChatTimerInput() {}

    public static void register() {
        ScreenInput.onScroll((s, x, y, dy) -> s instanceof ChatScreen && scroll(s.width, s.height, x, y, dy));
        ScreenInput.onClick((s, x, y, b) -> s instanceof ChatScreen && click(s.width, s.height, x, y));
    }

    private static boolean scroll(int sw, int sh, double x, double y, double dy) {
        TimerWidget w = Timers.at(x, y, sw, sh);
        return w != null && w.wheel(w.element(), dy > 0 ? 1 : -1);
    }

    private static boolean click(int sw, int sh, double x, double y) {
        TimerWidget w = Timers.at(x, y, sw, sh);
        if (w == null) return false;
        w.toggle(System.currentTimeMillis());
        return true;
    }
}
