package dev.henny.vibeyclient.ui;

import com.mojang.blaze3d.platform.Window;
import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.client.ClientCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * The global "UI scale" of the Vibey screens: while a Vibey screen is open the window's GUI scale is set to the chosen
 * factor (whole numbers only, so pixel art and text stay crisp) and restored from the game options when it closes.
 * Screens call {@link #apply(Screen)} in {@code init()} and {@link #restore()} in {@code removed()}.
 */
public final class UiScale {
    private UiScale() {}

    private static boolean overridden;

    /** Applies the setting to the window and updates the screen's GUI size. */
    public static void apply(Screen screen) {
        int want = GlobalSettings.uiScale.get().factor;
        Minecraft mc = Minecraft.getInstance();
        Window w = mc.getWindow();
        if (want <= 0) {
            if (overridden) restore();
            return;
        }
        want = Math.min(want, Math.max(1, w.calculateScale(0, mc.isEnforceUnicode())));
        if (want != ClientCompat.guiScale()) {
            set(w, want);
            overridden = true;
        }
        screen.width = w.getGuiScaledWidth();
        screen.height = w.getGuiScaledHeight();
    }

    /** Back to the game's own GUI scale. */
    public static void restore() {
        if (!overridden) return;
        overridden = false;
        Minecraft mc = Minecraft.getInstance();
        Window w = mc.getWindow();
        set(w, w.calculateScale(mc.options.guiScale().get(), mc.isEnforceUnicode()));
    }

    private static void set(Window w, int scale) {
        //? if >=1.21.6 {
        w.setGuiScale(scale);
        //?} else
        /*w.setGuiScale((double) scale);*/
    }

    /** Re-applies after the setting changed while a Vibey screen is open. */
    public static void refresh() {
        Screen s = ClientCompat.screen();
        if (s == null) return;
        restore();
        Window w = Minecraft.getInstance().getWindow();
        //? if >=1.21.11 {
        s.resize(w.getGuiScaledWidth(), w.getGuiScaledHeight());
        //?} else
        /*s.resize(Minecraft.getInstance(), w.getGuiScaledWidth(), w.getGuiScaledHeight());*/
    }
}
