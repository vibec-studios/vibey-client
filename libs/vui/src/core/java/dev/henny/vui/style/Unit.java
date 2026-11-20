package dev.henny.vui.style;

public final class Unit {
    public static final byte PX = 0;
    public static final byte PERCENT = 1;
    public static final byte AUTO = 2;
    public static final byte NONE = 3;

    private Unit() {
    }

    public static String name(byte unit) {
        return switch (unit) {
            case PX -> "px";
            case PERCENT -> "%";
            case AUTO -> "auto";
            default -> "none";
        };
    }
}
