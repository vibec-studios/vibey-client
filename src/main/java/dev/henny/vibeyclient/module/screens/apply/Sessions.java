package dev.henny.vibeyclient.module.screens.apply;

import dev.henny.vibeyclient.module.screens.Screens;
import net.minecraft.client.gui.screens.Screen;

import java.util.Map;
import java.util.WeakHashMap;

public final class Sessions {
    private static final Map<Screen, ScreenSession> OPEN = new WeakHashMap<>();

    private Sessions() {}

    public static ScreenSession get(Screen screen) {
        if (screen == null || !Screens.active() || Screens.blocked(screen)) return null;
        synchronized (OPEN) {
            return OPEN.computeIfAbsent(screen, ScreenSession::new);
        }
    }

    public static ScreenSession peek(Screen screen) {
        if (screen == null) return null;
        synchronized (OPEN) {
            return OPEN.get(screen);
        }
    }

    public static ScreenSession usable(Screen screen) {
        ScreenSession s = get(screen);
        return s == null || s.broken || Guard.broken(s.name) ? null : s;
    }

    public static void afterInit(Screen screen) {
        ScreenSession s = usable(screen);
        if (s == null) return;
        Guard.run(s, () -> LayoutApplier.apply(s));
    }

    public static void beforeReposition(Screen screen) {
        ScreenSession s = peek(screen);
        if (s == null || s.broken) return;
        Guard.run(s, () -> LayoutApplier.restore(s));
    }

    public static void reapply(ScreenSession s) {
        if (s == null || s.broken) return;
        Guard.run(s, () -> {
            LayoutApplier.restore(s);
            LayoutApplier.apply(s);
        });
    }

    public static void forget(Screen screen) {
        synchronized (OPEN) {
            OPEN.remove(screen);
        }
    }
}
