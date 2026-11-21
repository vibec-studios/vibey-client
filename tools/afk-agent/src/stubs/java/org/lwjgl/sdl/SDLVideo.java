package org.lwjgl.sdl;

public final class SDLVideo {
    public static long lastFlags;
    public static int shows;

    public static long SDL_CreateWindow(CharSequence title, int w, int h, long flags) {
        lastFlags = flags;
        return 77L;
    }

    public static boolean SDL_ShowWindow(long window) {
        shows++;
        return true;
    }

    public static boolean SDL_GL_SwapWindow(long window) {
        return true;
    }
}
