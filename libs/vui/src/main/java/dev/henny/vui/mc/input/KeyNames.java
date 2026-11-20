package dev.henny.vui.mc.input;

import com.mojang.blaze3d.platform.InputConstants;

final class KeyNames {
    private KeyNames() {
    }

    static String of(int glfwKey) {
        int code = KeyCodes.toNative(glfwKey);
        if (code < 0) return null;
        try {
            //? if >=26.3 {
            /*String name = InputConstants.Type.KEYBOARD.getOrCreate(code).getDisplayName().getString();
            *///?} else
            String name = InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName().getString();
            if (name == null || name.isBlank() || name.startsWith("key.")) return null;
            return name.length() == 1 ? name.toUpperCase() : name;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
