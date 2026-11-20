package dev.henny.vui.mc.input;

public final class KeyCodes {
    public static final int UNKNOWN = -1;

    private static final int[] SDL_TO_GLFW = SdlKeys.toGlfw();
    private static final int[] GLFW_TO_SDL = SdlKeys.invert(SDL_TO_GLFW);

    private KeyCodes() {
    }

    public static int fromNative(int code) {
        //? if >=26.3 {
        /*if (code < 0 || code >= SDL_TO_GLFW.length) return UNKNOWN;
        return SDL_TO_GLFW[code];
        *///?} else
        return code;
    }

    public static int toNative(int glfw) {
        //? if >=26.3 {
        /*if (glfw < 0 || glfw >= GLFW_TO_SDL.length) return UNKNOWN;
        return GLFW_TO_SDL[glfw];
        *///?} else
        return glfw;
    }
}
