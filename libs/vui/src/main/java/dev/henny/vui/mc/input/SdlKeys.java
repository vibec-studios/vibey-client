package dev.henny.vui.mc.input;

import java.util.Arrays;

final class SdlKeys {
    private static final int SDL_SIZE = 512;
    private static final int GLFW_SIZE = 349;

    private SdlKeys() {
    }

    static int[] toGlfw() {
        int[] m = new int[SDL_SIZE];
        Arrays.fill(m, KeyCodes.UNKNOWN);
        for (int i = 0; i < 26; i++) m[4 + i] = 'A' + i;
        for (int i = 0; i < 9; i++) m[30 + i] = '1' + i;
        m[39] = '0';
        map(m, 40, 257, 41, 256, 42, 259, 43, 258, 44, 32, 45, 45, 46, 61, 47, 91, 48, 93, 49, 92, 50, 92, 51, 59,
                52, 39, 53, 96, 54, 44, 55, 46, 56, 47, 57, 280);
        for (int i = 0; i < 12; i++) m[58 + i] = 290 + i;
        map(m, 70, 283, 71, 281, 72, 284, 73, 260, 74, 268, 75, 266, 76, 261, 77, 269, 78, 267, 79, 262, 80, 263,
                81, 264, 82, 265, 83, 282, 84, 331, 85, 332, 86, 333, 87, 334, 88, 335, 99, 330, 100, 162, 101, 348, 103, 336);
        for (int i = 0; i < 9; i++) m[89 + i] = 321 + i;
        m[98] = 320;
        for (int i = 0; i < 12; i++) m[104 + i] = 302 + i;
        map(m, 224, 341, 225, 340, 226, 342, 227, 343, 228, 345, 229, 344, 230, 346, 231, 347);
        return m;
    }

    static int[] invert(int[] sdlToGlfw) {
        int[] m = new int[GLFW_SIZE];
        Arrays.fill(m, KeyCodes.UNKNOWN);
        for (int sdl = sdlToGlfw.length - 1; sdl >= 0; sdl--) {
            int glfw = sdlToGlfw[sdl];
            if (glfw >= 0 && glfw < GLFW_SIZE) m[glfw] = sdl;
        }
        return m;
    }

    private static void map(int[] m, int... pairs) {
        for (int i = 0; i + 1 < pairs.length; i += 2) m[pairs[i]] = pairs[i + 1];
    }
}
