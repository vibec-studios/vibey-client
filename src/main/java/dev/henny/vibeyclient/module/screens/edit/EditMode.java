package dev.henny.vibeyclient.module.screens.edit;

import dev.henny.vibeyclient.module.screens.apply.ScreenSession;
import dev.henny.vibeyclient.module.screens.apply.Sessions;
import net.minecraft.client.gui.screens.Screen;

public final class EditMode {
    public static boolean drawPointer;
    private static EditSession current;

    private EditMode() {}

    public static EditSession current() {
        return current;
    }

    public static EditSession of(Screen screen) {
        EditSession e = current;
        return e != null && e.screen.screen == screen ? e : null;
    }

    public static boolean active(Screen screen) {
        return of(screen) != null;
    }

    public static boolean enter(Screen screen) {
        ScreenSession s = Sessions.usable(screen);
        if (s == null) return false;
        current = new EditSession(s);
        Sessions.reapply(s);
        return true;
    }

    public static void leave() {
        current = null;
    }

    public static boolean toggle(Screen screen) {
        if (active(screen)) {
            leave();
            return true;
        }
        return enter(screen);
    }

    public static void removed(Screen screen) {
        if (active(screen)) leave();
    }
}
