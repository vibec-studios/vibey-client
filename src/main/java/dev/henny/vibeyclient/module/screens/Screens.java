package dev.henny.vibeyclient.module.screens;

import dev.henny.vibeyclient.module.screens.core.Blocklist;
import dev.henny.vibeyclient.module.screens.core.LayoutStore;
import net.minecraft.client.gui.screens.Screen;

public final class Screens {
    private static final LayoutStore STORE = new LayoutStore();
    private static volatile ScreensModule module;
    private static volatile Blocklist blocklist = new Blocklist("");

    private Screens() {}

    static void attach(ScreensModule m) {
        module = m;
    }

    static void blocklist(String extra) {
        blocklist = new Blocklist(extra);
    }

    public static LayoutStore store() {
        return STORE;
    }

    public static ScreensModule module() {
        return module;
    }

    public static boolean active() {
        ScreensModule m = module;
        return m != null && m.isEnabled();
    }

    public static boolean blocked(Screen screen) {
        return blocklist.blocked(screen.getClass().getName());
    }
}
