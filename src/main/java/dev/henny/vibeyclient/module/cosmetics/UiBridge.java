package dev.henny.vibeyclient.module.cosmetics;

import net.minecraft.client.gui.screens.Screen;

import java.util.function.BooleanSupplier;

/**
 * Calls into the wardrobe/emote wheel UI ({@code module.cosmetics.ui.WardrobeUi}) by name, so the engine does not
 * break while the UI package is being built separately.
 */
public final class UiBridge {
    private UiBridge() {}

    private static final String UI = "dev.henny.vibeyclient.module.cosmetics.ui.WardrobeUi";

    static void call(String method) {
        try {
            Class.forName(UI).getMethod(method).invoke(null);
        } catch (ReflectiveOperationException | LinkageError e) {
            dev.henny.vibeyclient.Vibey.LOG.debug("WardrobeUi.{} unavailable: {}", method, e.toString());
        }
    }

    public static void openWardrobe(Screen parent) {
        try {
            Class.forName(UI).getMethod("openWardrobe", Screen.class).invoke(null, parent);
        } catch (ReflectiveOperationException | LinkageError e) {
            dev.henny.vibeyclient.Vibey.LOG.warn("Wardrobe UI unavailable: {}", e.toString());
        }
    }

    public static void openWheel(BooleanSupplier keyHeld) {
        try {
            Class.forName(UI).getMethod("openWheel", BooleanSupplier.class).invoke(null, keyHeld);
        } catch (ReflectiveOperationException | LinkageError e) {
            dev.henny.vibeyclient.Vibey.LOG.warn("Emote wheel unavailable: {}", e.toString());
        }
    }

    public static void openWheelEditor(Screen parent) {
        try {
            Class.forName(UI).getMethod("openWheelEditor", Screen.class).invoke(null, parent);
        } catch (ReflectiveOperationException | LinkageError e) {
            dev.henny.vibeyclient.Vibey.LOG.warn("Wheel editor unavailable: {}", e.toString());
        }
    }
}
