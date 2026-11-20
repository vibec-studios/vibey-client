package dev.henny.vui.style;

public record StyleValue(float number, byte unit, int color, Object object, byte special) {
    public static final byte NORMAL = 0;
    public static final byte INHERIT = 1;
    public static final byte INITIAL = 2;

    public static final StyleValue INHERIT_VALUE = new StyleValue(0, Unit.PX, 0, null, INHERIT);
    public static final StyleValue INITIAL_VALUE = new StyleValue(0, Unit.PX, 0, null, INITIAL);

    public static StyleValue length(float value, byte unit) {
        return new StyleValue(value, unit, 0, null, NORMAL);
    }

    public static StyleValue number(float value) {
        return new StyleValue(value, Unit.PX, 0, null, NORMAL);
    }

    public static StyleValue color(int argb) {
        return new StyleValue(0, Unit.PX, argb, null, NORMAL);
    }

    public static StyleValue object(Object value) {
        return new StyleValue(0, Unit.PX, 0, value, NORMAL);
    }

    public boolean isInherit() {
        return special == INHERIT;
    }

    public boolean isInitial() {
        return special == INITIAL;
    }
}
