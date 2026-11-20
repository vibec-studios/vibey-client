package dev.henny.vui.style;

public final class ComputedStyle {
    private static final Prop[] PROPS = Prop.values();
    private static final ComputedStyle INITIAL = createInitial();

    private final float[] numbers = new float[PROPS.length];
    private final byte[] units = new byte[PROPS.length];
    private final int[] colors = new int[PROPS.length];
    private final Object[] objects = new Object[PROPS.length];

    public static ComputedStyle initial() {
        ComputedStyle s = new ComputedStyle();
        s.copyFrom(INITIAL);
        return s;
    }

    private static ComputedStyle createInitial() {
        ComputedStyle s = new ComputedStyle();
        for (Prop p : PROPS) s.set(p, ValueParser.initial(p));
        return s;
    }

    public void set(Prop p, StyleValue v) {
        int i = p.ordinal();
        numbers[i] = v.number();
        units[i] = v.unit();
        colors[i] = v.color();
        objects[i] = v.object();
    }

    public void copy(Prop p, ComputedStyle from) {
        int i = p.ordinal();
        numbers[i] = from.numbers[i];
        units[i] = from.units[i];
        colors[i] = from.colors[i];
        objects[i] = from.objects[i];
    }

    public void copyFrom(ComputedStyle from) {
        System.arraycopy(from.numbers, 0, numbers, 0, numbers.length);
        System.arraycopy(from.units, 0, units, 0, units.length);
        System.arraycopy(from.colors, 0, colors, 0, colors.length);
        System.arraycopy(from.objects, 0, objects, 0, objects.length);
    }

    public void reset(Prop p) {
        copy(p, INITIAL);
    }

    public boolean sameAs(Prop p, ComputedStyle other) {
        int i = p.ordinal();
        return numbers[i] == other.numbers[i] && units[i] == other.units[i] && colors[i] == other.colors[i]
                && java.util.Objects.equals(objects[i], other.objects[i]);
    }

    public float number(Prop p) {
        return numbers[p.ordinal()];
    }

    public byte unit(Prop p) {
        return units[p.ordinal()];
    }

    public int color(Prop p) {
        return colors[p.ordinal()];
    }

    public Object object(Prop p) {
        return objects[p.ordinal()];
    }

    public String keyword(Prop p) {
        Object o = objects[p.ordinal()];
        return o instanceof String s ? s : "";
    }

    public boolean is(Prop p, String keyword) {
        return keyword.equals(objects[p.ordinal()]);
    }

    public boolean isAuto(Prop p) {
        return units[p.ordinal()] == Unit.AUTO;
    }

    public void setNumber(Prop p, float value) {
        numbers[p.ordinal()] = value;
    }

    public void setColor(Prop p, int argb) {
        colors[p.ordinal()] = argb;
    }

    public void setLength(Prop p, float value, byte unit) {
        numbers[p.ordinal()] = value;
        units[p.ordinal()] = unit;
    }

    public void setObject(Prop p, Object value) {
        objects[p.ordinal()] = value;
    }

    public String describe(Prop p) {
        return switch (p.kind()) {
            case LENGTH -> switch (unit(p)) {
                case Unit.AUTO -> "auto";
                case Unit.NONE -> "none";
                case Unit.PERCENT -> dev.henny.vui.expr.Values.formatNumber(number(p)) + "%";
                default -> dev.henny.vui.expr.Values.formatNumber(number(p));
            };
            case NUMBER, TIME -> dev.henny.vui.expr.Values.formatNumber(number(p));
            case COLOR -> ColorParser.format(color(p));
            default -> String.valueOf(object(p));
        };
    }
}
