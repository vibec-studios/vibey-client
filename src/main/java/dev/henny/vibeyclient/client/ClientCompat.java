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

    public static boolean shiftDown() {
        //? if >=1.21.9 {
        return Minecraft.getInstance().hasShiftDown();
        //?} else
        /*return Screen.hasShiftDown();*/
    }

    public static boolean controlDown() {
        //? if >=1.21.9 {
        return Minecraft.getInstance().hasControlDown();
        //?} else
        /*return Screen.hasControlDown();*/
    }

    /** The current GUI scale (screen pixels per GUI pixel). */
    public static int guiScale() {
        // double until 1.21.5, int since 1.21.6
        return Math.max(1, (int) Math.round((double) Minecraft.getInstance().getWindow().getGuiScale()));
    }

    /** The vanilla options screen (26.1/26.2 take an extra "opened in game" flag). */
    public static Screen optionsScreen(Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        //? if >=26.1 && <26.3 {
        /*return new net.minecraft.client.gui.screens.options.OptionsScreen(parent, mc.options, mc.level != null);
        *///?} else
        return new net.minecraft.client.gui.screens.options.OptionsScreen(parent, mc.options);
    }

    /** Asks "open this link?" (vanilla dialog) and opens it in the browser; returns to {@code parent}. */
    public static void openLink(Screen parent, String url) {
        net.minecraft.client.gui.screens.ConfirmLinkScreen.confirmLinkNow(parent, java.net.URI.create(url));
    }

    /** Name of the logged-in Minecraft account. */
    public static String playerName() {
        try {
            return Minecraft.getInstance().getUser().getName();
        } catch (RuntimeException e) {
            return "Player";
        }
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

    /** Other key mappings (vanilla, mods, Vibey) on the same key as {@code mapping}; empty if unbound. */
    public static java.util.List<KeyMapping> conflicts(KeyMapping mapping) {
        java.util.List<KeyMapping> out = new java.util.ArrayList<>(2);
        if (mapping.isUnbound()) return out;
        for (KeyMapping other : Minecraft.getInstance().options.keyMappings) {
            if (other != mapping && !other.isUnbound() && other.same(mapping)) out.add(other);
        }
        return out;
    }

    /** Translated name of a key mapping ("Jump", "Open Vibey menu" …). */
    public static String mappingName(KeyMapping mapping) {
        return Component.translatable(mapping.getName()).getString();
    }
}
