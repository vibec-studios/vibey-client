package dev.henny.vibeyclient.client;

/**
 * GLFW key codes for keybind defaults. They are the same on every version, but LWJGL is not on the compile
 * classpath of every target (26.3), so use these instead of {@code org.lwjgl.glfw.GLFW}.
 */
public final class Keys {
    private Keys() {}

    public static final int UNBOUND = -1;
    public static final int SPACE = 32;
    public static final int A = 65, B = 66, C = 67, D = 68, E = 69, F = 70, G = 71, H = 72, I = 73, J = 74, K = 75,
            L = 76, M = 77, N = 78, O = 79, P = 80, Q = 81, R = 82, S = 83, T = 84, U = 85, V = 86, W = 87, X = 88,
            Y = 89, Z = 90;
    public static final int ESCAPE = 256, ENTER = 257, TAB = 258, BACKSPACE = 259, INSERT = 260, DELETE = 261;
    public static final int RIGHT = 262, LEFT = 263, DOWN = 264, UP = 265;
    public static final int F1 = 290, F2 = 291, F3 = 292, F4 = 293, F5 = 294, F6 = 295, F7 = 296, F8 = 297, F9 = 298,
            F10 = 299, F11 = 300, F12 = 301;
    public static final int LEFT_SHIFT = 340, LEFT_CONTROL = 341, LEFT_ALT = 342, RIGHT_SHIFT = 344,
            RIGHT_CONTROL = 345, RIGHT_ALT = 346;

    /**
     * The key code the running game uses for a GLFW code: the same up to 26.2; 26.3 (SDL) uses SDL scancodes, and an
     * out-of-range GLFW code there crashes KeyMapping.setAll. Unknown keys map to -1 (unbound).
     */
    public static int toNative(int glfw) {
        //? if >=26.3 {
        /*if (glfw < 0) return -1;
        if (glfw >= A && glfw <= Z) return 4 + (glfw - A);
        if (glfw >= 49 && glfw <= 57) return 30 + (glfw - 49); // 1..9
        if (glfw == 48) return 39;                              // 0
        if (glfw >= F1 && glfw <= F12) return 58 + (glfw - F1);
        return switch (glfw) {
            case ENTER -> 40;
            case ESCAPE -> 41;
            case BACKSPACE -> 42;
            case TAB -> 43;
            case SPACE -> 44;
            case INSERT -> 73;
            case DELETE -> 76;
            case RIGHT -> 79;
            case LEFT -> 80;
            case DOWN -> 81;
            case UP -> 82;
            case LEFT_CONTROL -> 224;
            case LEFT_SHIFT -> 225;
            case LEFT_ALT -> 226;
            case RIGHT_CONTROL -> 228;
            case RIGHT_SHIFT -> 229;
            case RIGHT_ALT -> 230;
            default -> -1;
        };
        *///?} else
        return glfw;
    }

    /**
     * Keys that have no GLFW constant here come out of {@link #fromNative} as {@code NATIVE_ONLY + native code}, so
     * they never equal a GLFW constant but can still be bound ({@link #toNativeAny}).
     */
    public static final int NATIVE_ONLY = 1 << 20;

    /**
     * The GLFW code of a key code the running game reports (screen key events): the same up to 26.2; on 26.3 (SDL
     * scancodes) the keys of {@link #toNative} map back, any other key becomes {@code NATIVE_ONLY + code}.
     */
    public static int fromNative(int nativeCode) {
        //? if >=26.3 {
        /*if (nativeCode < 0) return -1;
        if (nativeCode >= 4 && nativeCode <= 29) return A + (nativeCode - 4);
        if (nativeCode >= 30 && nativeCode <= 38) return 49 + (nativeCode - 30); // 1..9
        if (nativeCode == 39) return 48;                                          // 0
        if (nativeCode >= 58 && nativeCode <= 69) return F1 + (nativeCode - 58);
        return switch (nativeCode) {
            case 40 -> ENTER;
            case 41 -> ESCAPE;
            case 42 -> BACKSPACE;
            case 43 -> TAB;
            case 44 -> SPACE;
            case 73 -> INSERT;
            case 76 -> DELETE;
            case 79 -> RIGHT;
            case 80 -> LEFT;
            case 81 -> DOWN;
            case 82 -> UP;
            case 88 -> 335; // keypad enter
            case 224 -> LEFT_CONTROL;
            case 225 -> LEFT_SHIFT;
            case 226 -> LEFT_ALT;
            case 228 -> RIGHT_CONTROL;
            case 229 -> RIGHT_SHIFT;
            case 230 -> RIGHT_ALT;
            default -> NATIVE_ONLY + nativeCode;
        };
        *///?} else
        return nativeCode;
    }

    /** Inverse of {@link #fromNative}: the game's key code for a GLFW code or a {@code NATIVE_ONLY} code. */
    public static int toNativeAny(int code) {
        if (code >= NATIVE_ONLY) return code - NATIVE_ONLY;
        return toNative(code);
    }

    /** Mouse buttons as our UI code numbers them (GLFW): 0 left, 1 right, 2 middle, 3/4 side buttons. */
    public static final int MOUSE_LEFT = 0, MOUSE_RIGHT = 1, MOUSE_MIDDLE = 2;

    /**
     * The GLFW number ({@link #MOUSE_LEFT} ...) of a mouse button the game reports (MouseButtonEvent#button, Fabric /
     * NeoForge screen mouse events). Up to 26.2 they are GLFW numbers; 26.3 (SDL) reports SDL numbers: 1 left,
     * 2 middle, 3 right, 4/5 side buttons. Without this, a left click on 26.3 arrives as "button 1" (right) and no
     * Vibey widget reacts.
     */
    public static int mouseFromNative(int button) {
        //? if >=26.3 {
        /*return switch (button) {
            case 1 -> MOUSE_LEFT;
            case 2 -> MOUSE_MIDDLE;
            case 3 -> MOUSE_RIGHT;
            default -> button - 1;
        };
        *///?} else
        return button;
    }

    /** Inverse of {@link #mouseFromNative}: the number the running game uses for a GLFW mouse button. */
    public static int mouseToNative(int button) {
        //? if >=26.3 {
        /*return switch (button) {
            case MOUSE_LEFT -> 1;
            case MOUSE_MIDDLE -> 2;
            case MOUSE_RIGHT -> 3;
            default -> button + 1;
        };
        *///?} else
        return button;
    }
}
