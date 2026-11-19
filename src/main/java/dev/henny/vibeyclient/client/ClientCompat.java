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

    /** Opens a folder (or file) in the system file manager. */
    public static void openFolder(java.nio.file.Path path) {
        //? if >=26.3 {
        /*com.mojang.blaze3d.Blaze3D.openPath(path);
        *///?}
        //? if >=1.21.11 <26.3 {
        net.minecraft.util.Util.getPlatform().openPath(path);
        //?}
        //? if <1.21.11
        /*net.minecraft.Util.getPlatform().openPath(path);*/
    }

    /** The system clipboard text ("" when empty or unavailable). */
    public static String clipboard() {
        try {
            String s = Minecraft.getInstance().keyboardHandler.getClipboard();
            return s == null ? "" : s;
        } catch (RuntimeException e) {
            return "";
        }
    }

    public static void setClipboard(String s) {
        Minecraft.getInstance().keyboardHandler.setClipboard(s);
    }

    /**
     * The letter a key code stands for on the player's keyboard layout (GLFW key codes are US positions: on a German
     * layout the key labelled Z reports Y). Upper case; the US letter when the layout name is unknown.
     */
    public static int layoutLetter(int glfwKey) {
        if (glfwKey < 'A' || glfwKey > 'Z') return glfwKey;
        try {
            //? if >=26.3 {
            /*String n = InputConstants.Type.KEYBOARD.getOrCreate(dev.henny.vibeyclient.client.Keys.toNative(glfwKey)).getDisplayName().getString();
            *///?} else
            String n = InputConstants.Type.KEYSYM.getOrCreate(dev.henny.vibeyclient.client.Keys.toNative(glfwKey)).getDisplayName().getString();
            if (n.length() == 1) {
                char c = Character.toUpperCase(n.charAt(0));
                if (c >= 'A' && c <= 'Z') return c;
            }
        } catch (RuntimeException e) {
            // keep the US letter
        }
        return glfwKey;
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

    /**
     * Lower edge (GUI px from the top) of the vanilla toasts in the top right corner (advancements, recipes, "chat
     * messages can't be verified", ...), 0 if there are none. A toast that is sliding out counts with the part that is
     * still on screen, so a stack laid out below follows it up smoothly. Client thread.
     */
    public static float vanillaToastBottom() {
        Minecraft mc = Minecraft.getInstance();
        //? if >=26.2 {
        /*Object manager = mc.gui.toastManager();
        *///?} else if >=1.21.2 {
        Object manager = mc.getToastManager();
        //?} else
        /*Object manager = mc.getToasts();*/
        if (!(manager instanceof dev.henny.vibeyclient.mixin.client.ToastManagerAccessor access)) return 0f;
        float bottom = 0f;
        try {
            for (Object o : access.vibeyclient$visible()) {
                if (!(o instanceof dev.henny.vibeyclient.mixin.client.ToastInstanceAccessor t)) continue;
                net.minecraft.client.gui.components.toasts.Toast toast = t.vibeyclient$toast();
                if (toast == null) continue;
                //? if >=1.21.6 {
                float top = toast.yPos(t.vibeyclient$slot());
                //?} else
                /*float top = t.vibeyclient$slot() * 32f;*/
                //? if >=1.21.2 {
                float shown = t.vibeyclient$visibility() == net.minecraft.client.gui.components.toasts.Toast.Visibility.SHOW
                        ? 1f : Math.max(0f, Math.min(1f, t.vibeyclient$visiblePortion()));
                //?} else {
                /*float shown = t.vibeyclient$visibility() == net.minecraft.client.gui.components.toasts.Toast.Visibility.SHOW ? 1f : 0.5f;
                *///?}
                bottom = Math.max(bottom, (top + toast.height()) * shown);
            }
        } catch (RuntimeException e) {
            return 0f; // list changed while iterating (never on the client thread) – skip this frame
        }
        return bottom;
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

    /** Whether {@code keyCode} (GLFW, or a {@code Keys.NATIVE_ONLY} code from a screen) is the key of {@code mapping}. */
    public static boolean matches(KeyMapping mapping, int keyCode) {
        keyCode = Keys.toNativeAny(keyCode);
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
