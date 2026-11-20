package dev.henny.r3.mc.input;

import com.mojang.blaze3d.platform.InputConstants;

public final class KeyLayout {
    private KeyLayout() {}

    public static int letter(int glfwKey, int nativeKey) {
        if (glfwKey < 'A' || glfwKey > 'Z') return glfwKey;
        try {
            //? if >=26.3 {
            /*String name = InputConstants.Type.KEYBOARD.getOrCreate(nativeKey).getDisplayName().getString();
            *///?} else
            String name = InputConstants.Type.KEYSYM.getOrCreate(nativeKey).getDisplayName().getString();
            if (name.length() == 1) {
                char c = Character.toUpperCase(name.charAt(0));
                if (c >= 'A' && c <= 'Z') return c;
            }
        } catch (RuntimeException e) {
            return glfwKey;
        }
        return glfwKey;
    }
}
