package dev.henny.vibeyclient.ui.toast;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.service.Notify;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.ui.Gfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;
//? if fabric {
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
//?} else {
/*import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
*///?}

/**
 * Wires the toast stack into the game: it is the {@link Notify} sink, draws after every screen (and on the HUD via
 * {@link #renderHud}), takes clicks on toasts before the screen does, and offers screen key listeners (the social
 * module opens its sidebar with them over menus).
 */
public final class ToastHooks {
    private ToastHooks() {}

    private static boolean done;
    private static final List<BiPredicate<Screen, Integer>> KEY_LISTENERS = new CopyOnWriteArrayList<>();
    /** Extra overlays drawn after a screen (before the toasts), e.g. badges on menu buttons. */
    private static final List<ScreenOverlay> OVERLAYS = new CopyOnWriteArrayList<>();

    public interface ScreenOverlay {
        void render(Screen screen, Gfx g, int mouseX, int mouseY);
    }

    /** Once from the client core's init. */
    public static synchronized void init() {
        if (done) return;
        done = true;
        Lang.addFile("toast");
        Notify.setSink(ToastStack::show);
        //? if fabric {
        ScreenEvents.AFTER_INIT.register((mc, screen, w, h) -> {
            //? if >=26.1 {
            /*ScreenEvents.afterExtract(screen).register((s, g, mx, my, d) -> afterScreen(s, new Gfx(g, mc.font), mx, my));
            *///?} else
            ScreenEvents.afterRender(screen).register((s, g, mx, my, d) -> afterScreen(s, new Gfx(g, mc.font), mx, my));
            //? if >=1.21.9 {
            ScreenMouseEvents.allowMouseClick(screen).register((s, e) -> !ToastStack.click(e.x(), e.y(), dev.henny.vibeyclient.client.Keys.mouseFromNative(e.button())));
            ScreenKeyboardEvents.afterKeyPress(screen).register((s, e) -> screenKey(s, e.key()));
            //?} else {
            /*ScreenMouseEvents.allowMouseClick(screen).register((s, x, y, b) -> !ToastStack.click(x, y, b));
            ScreenKeyboardEvents.afterKeyPress(screen).register((s, key, scan, mods) -> screenKey(s, key));
            *///?}
        });
        //?} else {
        /*NeoForge.EVENT_BUS.addListener((ScreenEvent.Render.Post e) ->
                afterScreen(e.getScreen(), new Gfx(e.getGuiGraphics(), Minecraft.getInstance().font), e.getMouseX(), e.getMouseY()));
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseButtonPressed.Pre e) -> {
            if (ToastStack.click(e.getMouseX(), e.getMouseY(), dev.henny.vibeyclient.client.Keys.mouseFromNative(e.getButton()))) e.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.KeyPressed.Post e) -> screenKey(e.getScreen(), neoKey(e)));
        *///?}
    }

    //? if neoforge && >=1.21.9 {
    /*private static int neoKey(ScreenEvent.KeyPressed e) {
        return e.getKeyEvent().key();
    }
    *///?}
    //? if neoforge && <1.21.9 {
    /*private static int neoKey(ScreenEvent.KeyPressed e) {
        return e.getKeyCode();
    }
    *///?}

    /** A key pressed on a screen (after the screen handled it); listeners return true if they used it. */
    public static void addScreenKeyListener(BiPredicate<Screen, Integer> listener) {
        KEY_LISTENERS.add(listener);
    }

    public static void addScreenOverlay(ScreenOverlay overlay) {
        OVERLAYS.add(overlay);
    }

    private static void screenKey(Screen screen, int nativeKey) {
        int key = dev.henny.vibeyclient.client.Keys.fromNative(nativeKey);
        for (BiPredicate<Screen, Integer> l : KEY_LISTENERS) {
            try {
                if (l.test(screen, key)) return;
            } catch (Throwable t) {
                dev.henny.vibeyclient.Vibey.LOG.error("Screen key listener failed", t);
            }
        }
    }

    private static void afterScreen(Screen screen, Gfx g, int mx, int my) {
        for (ScreenOverlay o : OVERLAYS) {
            try {
                o.render(screen, g, mx, my);
            } catch (Throwable t) {
                dev.henny.vibeyclient.Vibey.LOG.error("Screen overlay failed", t);
            }
        }
        ToastStack.render(g, mx, my);
    }

    /** From the HUD hook: toasts in game when no screen is open (the HUD hook itself respects F1). */
    public static void renderHud(Gfx g) {
        if (ClientCompat.screen() != null) return;
        ToastStack.render(g, -1, -1);
    }
}
