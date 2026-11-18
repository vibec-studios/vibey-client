package dev.henny.vibeyclient.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
//? if >=1.21.9
import dev.henny.vibeyclient.Compat;

/**
 * Client calls whose shape changed between Minecraft 1.21.1 and 26.3 (see also ui/Gfx and ui/BaseScreen). Put new
 * version differences here so the rest of the code reads the same on every version.
 */
public final class ClientCompat {
    private ClientCompat() {}

    public static void openScreen(Screen screen) {
        //? if >=26.2 {
        /*Minecraft.getInstance().setScreenAndShow(screen);
        *///?} else
        Minecraft.getInstance().setScreen(screen);
    }

    /** The screen that is open, or null. */
    public static Screen screen() {
        //? if >=26.2 {
        /*return Minecraft.getInstance().gui.screen();
        *///?} else
        return Minecraft.getInstance().screen;
    }

    /** F1: everything but the world hidden. */
    public static boolean hudHidden() {
        //? if >=26.2 {
        /*return Minecraft.getInstance().gui.hud.isHidden();
        *///?} else
        return Minecraft.getInstance().options.hideGui;
    }

    public static boolean inWorld() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.player != null;
    }

    // -------------------------------------------------------------------------------------------- key mappings

    //? if >=1.21.9 {
    private static KeyMapping.Category category;

    public static KeyMapping.Category category() {
        if (category == null) {
            //? if fabric {
            category = KeyMapping.Category.register(Compat.id("main"));
            //?} else
            /*category = new KeyMapping.Category(Compat.id("main"));*/
        }
        return category;
    }
    //?}

    /** A new key mapping in the "Vibey Client" category (not registered yet, see {@link Keybinds}). */
    public static KeyMapping newKeyMapping(String name, int keyCode) {
        keyCode = Keys.toNative(keyCode);
        //? if >=1.21.9 {
        KeyMapping m = new KeyMapping(name, keyCode, category());
        //?} else
        /*KeyMapping m = new KeyMapping(name, keyCode, "key.categories.vibeyclient");*/
        // unbound: the real UNKNOWN key (26.3 crashes in KeyMapping.setAll on a keyboard key with code -1)
        if (keyCode < 0) m.setKey(InputConstants.UNKNOWN);
        return m;
    }

    public static void rebind(KeyMapping mapping, int keyCode) {
        //? if >=26.3 {
        /*mapping.setKey(keyCode < 0 ? InputConstants.UNKNOWN : InputConstants.Type.KEYBOARD.getOrCreate(keyCode));
        *///?} else
        mapping.setKey(keyCode < 0 ? InputConstants.UNKNOWN : InputConstants.Type.KEYSYM.getOrCreate(keyCode));
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
    }

    /** Whether {@code keyCode} (GLFW) is the key of {@code mapping}. */
    public static boolean matches(KeyMapping mapping, int keyCode) {
        //? if >=1.21.9 {
        return mapping.matches(new net.minecraft.client.input.KeyEvent(keyCode, 0, 0));
        //?} else
        /*return mapping.matches(keyCode, 0);*/
    }

    public static Component keyName(KeyMapping mapping) {
        return mapping.getTranslatedKeyMessage();
    }

    public static boolean isUnbound(KeyMapping mapping) {
        return mapping.isUnbound();
    }
}
