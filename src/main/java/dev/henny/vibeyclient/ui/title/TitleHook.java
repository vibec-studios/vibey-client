package dev.henny.vibeyclient.ui.title;

import dev.henny.vibeyclient.Smoke;
import dev.henny.vibeyclient.Vibey;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * Shows the first-start {@link OnboardingScreen} once, in front of the (vanilla) title screen. Called from the core
 * mixin on {@code Minecraft#setScreen} (26.2+: {@code Gui#setScreen}). The title screen itself stays vanilla; it only
 * gets the small Vibey buttons in a corner ({@link TitleButtons}).
 */
public final class TitleHook {
    private TitleHook() {}

    public static Screen replace(Screen screen) {
        if (!(screen instanceof TitleScreen title) || !enabled() || !CoreMenus.needsOnboarding()) return screen;
        try {
            return new OnboardingScreen(title);
        } catch (Throwable t) {
            Vibey.LOG.error("Vibey welcome screen failed, showing the title screen", t);
            return screen;
        }
    }

    /** Not inside a test harness that waits for the vanilla title screen. */
    public static boolean enabled() {
        if (System.getProperty("fabric.client.gametest") != null) return false; // Fabric's runner waits for TitleScreen
        return !Smoke.enabled();
    }
}
