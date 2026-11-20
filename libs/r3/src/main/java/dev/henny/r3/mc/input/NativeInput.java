package dev.henny.r3.mc.input;

public final class NativeInput {
    public static final int PRESS = 1;
    public static final int RELEASE = 0;
    public static final int REPEAT = 2;
    public static final int UNKNOWN = -1;

    private NativeInput() {}

    public static int mouseToGlfw(int button) {
        //? if >=26.3 {
        /*return switch (button) {
            case 1 -> 0;
            case 2 -> 2;
            case 3 -> 1;
            default -> button - 1;
        };
        *///?} else
        return button;
    }

    public static int keyToGlfw(int key) {
        //? if >=26.3 {
        /*return SdlKeys.toGlfw(key);
        *///?} else
        return key;
    }
}
