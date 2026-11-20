package dev.henny.vibeyclient.module.screens.hook;

import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.screens.ScreensModule;
import dev.henny.vibeyclient.module.screens.apply.Guard;
import dev.henny.vibeyclient.module.screens.apply.ScreenSession;
import dev.henny.vibeyclient.module.screens.edit.EditMode;
import dev.henny.vibeyclient.module.screens.edit.EditOverlay;
import dev.henny.vibeyclient.module.screens.edit.EditSession;
import dev.henny.vibeyclient.module.screens.edit.KeyInput;
import dev.henny.vibeyclient.module.screens.edit.Pencil;
import dev.henny.vibeyclient.module.screens.edit.PointerInput;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

final class ScreenInput {
    private ScreenInput() {}

    static void render(Screen screen, GuiGraphics g, int mx, int my) {
        ScreenSession s = ScreenHooks.session(screen);
        if (s == null) return;
        Guard.run(s, () -> {
            EditSession e = EditMode.of(screen);
            if (e != null) EditOverlay.draw(e, g, mx, my);
            else if (Pencil.shown(ScreensModule.get())) Pencil.draw(s, g, mx, my);
        });
    }

    static boolean click(Screen screen, double x, double y, int button) {
        ScreenSession s = ScreenHooks.session(screen);
        if (s == null) return false;
        boolean[] used = {false};
        Guard.run(s, () -> {
            EditSession e = EditMode.of(screen);
            if (e != null) {
                PointerInput.press(e, x, y, button);
                used[0] = true;
            } else if (button == 0 && Pencil.shown(ScreensModule.get()) && Pencil.hit(s, x, y)) {
                used[0] = EditMode.enter(screen);
            }
        });
        return used[0];
    }

    static boolean release(Screen screen, double x, double y) {
        EditSession e = EditMode.of(screen);
        if (e == null) return false;
        Guard.run(e.screen(), () -> PointerInput.release(e, x, y));
        return true;
    }

    static boolean drag(Screen screen, double x, double y) {
        EditSession e = EditMode.of(screen);
        if (e == null) return false;
        Guard.run(e.screen(), () -> PointerInput.drag(e, x, y));
        return true;
    }

    static boolean scroll(Screen screen, double x, double y, double amount) {
        EditSession e = EditMode.of(screen);
        if (e == null) return false;
        Guard.run(e.screen(), () -> PointerInput.wheel(e, x, y, amount));
        return true;
    }

    static boolean key(Screen screen, int key, boolean ctrl, boolean shift) {
        ScreenSession s = ScreenHooks.session(screen);
        ScreensModule m = ScreensModule.get();
        if (s == null || m == null) return false;
        if (ClientCompat.matches(m.editKey.mapping(), dev.henny.vibeyclient.client.Keys.fromNative(key))) {
            boolean[] used = {false};
            Guard.run(s, () -> used[0] = EditMode.toggle(screen));
            return used[0];
        }
        EditSession e = EditMode.of(screen);
        if (e == null) return false;
        Guard.run(s, () -> KeyInput.key(e, key, ctrl, shift));
        return true;
    }
}
