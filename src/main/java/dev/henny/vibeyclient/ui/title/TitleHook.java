package dev.henny.vibeyclient.ui.title;

import dev.henny.vibeyclient.Smoke;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.GlobalSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * Swaps the vanilla title screen for {@link VibeyTitleScreen} (and the first-start {@link OnboardingScreen}) whenever
 * the game opens one (startup, disconnect, "back to title" …). Called from the core mixin on
 * {@code Minecraft#setScreen} (26.2+: {@code Gui#setScreen}). With the "Vibey main menu" setting off, or while the
 * player asked for the classic menu, the vanilla screen stays (it then gets a small Vibey button, see
 * {@link #decorateVanilla}).
 */
public final class TitleHook {
    private TitleHook() {}

    /** Set when the player chose "Classic menu": the next vanilla title screen is kept once. */
    private static boolean keepVanillaOnce;

    public static Screen replace(Screen screen) {
        if (screen == null) {
            // Outside a world vanilla turns setScreen(null) into a new TitleScreen after this hook ran (e.g. a menu
            // opened from the title with a null parent is closed): hand back ours instead.
            if (!enabled() || net.minecraft.client.Minecraft.getInstance().level != null) return null;
            screen = new TitleScreen();
        }
        if (!(screen instanceof TitleScreen title) || !enabled()) return screen;
        if (keepVanillaOnce) {
            keepVanillaOnce = false;
            return screen;
        }
        try {
            boolean fading = fadeIn(title);
            VibeyTitleScreen vibey = new VibeyTitleScreen(fading);
            if (CoreMenus.needsOnboarding()) return new OnboardingScreen(vibey);
            return vibey;
        } catch (Throwable t) {
            Vibey.LOG.error("Vibey title screen failed, keeping the vanilla one", t);
            return screen;
        }
    }

    /** The replacement is on (setting), and we are not inside a test harness that waits for the vanilla screen. */
    public static boolean enabled() {
        if (!GlobalSettings.vibeyMainMenu.on()) return false;
        if (System.getProperty("fabric.client.gametest") != null) return false; // Fabric's runner waits for TitleScreen
        return !Smoke.enabled();
    }

    /** Opens the vanilla title screen once (e.g. to reach buttons other mods put there). */
    public static void openVanilla() {
        keepVanillaOnce = true;
        dev.henny.vibeyclient.client.ClientCompat.openScreen(new TitleScreen());
    }

    private static boolean fadeIn(TitleScreen title) {
        // Vanilla only fades in on the very first title screen after loading; we do the same.
        return !shownOnce;
    }

    static boolean shownOnce;
}
