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
}
