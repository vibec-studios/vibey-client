package dev.henny.vibeyclient.client;

import net.minecraft.client.gui.screens.Screen;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
//? if fabric {
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
//?} else {
/*import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
*///?}

public final class ScreenInput {
    private ScreenInput() {}

    public interface Click {
        boolean click(Screen screen, double x, double y, int button);
    }

    public interface Scroll {
        boolean scroll(Screen screen, double x, double y, double amount);
    }

    private static final List<Click> CLICKS = new CopyOnWriteArrayList<>();
    private static final List<Scroll> SCROLLS = new CopyOnWriteArrayList<>();
    private static boolean wired;

    public static void onClick(Click listener) {
        CLICKS.add(listener);
        wire();
    }

    public static void onScroll(Scroll listener) {
        SCROLLS.add(listener);
        wire();
    }

    private static boolean click(Screen s, double x, double y, int button) {
        for (Click c : CLICKS) if (c.click(s, x, y, button)) return true;
        return false;
    }

    private static boolean scroll(Screen s, double x, double y, double amount) {
        for (Scroll c : SCROLLS) if (c.scroll(s, x, y, amount)) return true;
        return false;
    }

    private static synchronized void wire() {
        if (wired) return;
        wired = true;
        //? if fabric {
        ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> {
            //? if >=1.21.9 {
            ScreenMouseEvents.allowMouseClick(screen).register((s, e) -> !click(s, e.x(), e.y(), Keys.mouseFromNative(e.button())));
            //?} else
            /*ScreenMouseEvents.allowMouseClick(screen).register((s, x, y, b) -> !click(s, x, y, b));*/
            ScreenMouseEvents.allowMouseScroll(screen).register((s, x, y, dx, dy) -> dy == 0 || !scroll(s, x, y, dy));
        });
        //?} else {
        /*NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseButtonPressed.Pre e) -> {
            if (click(e.getScreen(), e.getMouseX(), e.getMouseY(), Keys.mouseFromNative(e.getButton()))) e.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseScrolled.Pre e) -> {
            if (e.getScrollDeltaY() != 0 && scroll(e.getScreen(), e.getMouseX(), e.getMouseY(), e.getScrollDeltaY())) e.setCanceled(true);
        });
        *///?}
    }
}
