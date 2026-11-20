package dev.henny.vui.mc.input;

public final class MouseButtons {
    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int MIDDLE = 2;

    private MouseButtons() {
    }

    public static int fromNative(int button) {
        //? if >=26.3 {
        /*return switch (button) {
            case 1 -> LEFT;
            case 2 -> MIDDLE;
            case 3 -> RIGHT;
            default -> button - 1;
        };
        *///?} else
        return button;
    }
}
