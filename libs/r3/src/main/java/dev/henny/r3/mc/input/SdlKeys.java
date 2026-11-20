package dev.henny.r3.mc.input;

public final class SdlKeys {
    private static final int GLFW_A = 65;
    private static final int GLFW_F1 = 290;

    private SdlKeys() {}

    public static int toGlfw(int scancode) {
        if (scancode < 0) return NativeInput.UNKNOWN;
        if (scancode >= 4 && scancode <= 29) return GLFW_A + scancode - 4;
        if (scancode >= 30 && scancode <= 38) return '1' + scancode - 30;
        if (scancode == 39) return '0';
        if (scancode >= 58 && scancode <= 69) return GLFW_F1 + scancode - 58;
        return switch (scancode) {
            case 40 -> 257;
            case 41 -> 256;
            case 42 -> 259;
            case 43 -> 258;
            case 44 -> 32;
            case 73 -> 260;
            case 76 -> 261;
            case 79 -> 262;
            case 80 -> 263;
            case 81 -> 264;
            case 82 -> 265;
            case 88 -> 335;
            case 224 -> 341;
            case 225 -> 340;
            case 226 -> 342;
            case 228 -> 345;
            case 229 -> 344;
            case 230 -> 346;
            default -> NativeInput.UNKNOWN;
        };
    }
}
