package dev.henny.vui.layout;

import dev.henny.vui.style.ComputedStyle;
import dev.henny.vui.style.Prop;
import dev.henny.vui.style.Unit;

public final class Lengths {
    private Lengths() {
    }

    public static float resolve(ComputedStyle s, Prop p, float base) {
        byte unit = s.unit(p);
        if (unit == Unit.PX) return s.number(p);
        if (unit == Unit.PERCENT) return Float.isFinite(base) ? s.number(p) * base / 100f : Float.NaN;
        return Float.NaN;
    }

    public static float orZero(ComputedStyle s, Prop p, float base) {
        float v = resolve(s, p, base);
        return Float.isNaN(v) ? 0f : v;
    }

    public static float max(ComputedStyle s, Prop p, float base) {
        float v = resolve(s, p, base);
        return Float.isNaN(v) ? Float.POSITIVE_INFINITY : v;
    }

    public static float clamp(float value, float min, float max) {
        float v = Math.min(value, max);
        return Math.max(v, min);
    }

    public static void resolveBox(LayoutBox b, ComputedStyle s, float base) {
        b.paddingTop = orZero(s, Prop.PADDING_TOP, base);
        b.paddingRight = orZero(s, Prop.PADDING_RIGHT, base);
        b.paddingBottom = orZero(s, Prop.PADDING_BOTTOM, base);
        b.paddingLeft = orZero(s, Prop.PADDING_LEFT, base);
        b.marginTop = orZero(s, Prop.MARGIN_TOP, base);
        b.marginRight = orZero(s, Prop.MARGIN_RIGHT, base);
        b.marginBottom = orZero(s, Prop.MARGIN_BOTTOM, base);
        b.marginLeft = orZero(s, Prop.MARGIN_LEFT, base);
        b.borderTop = orZero(s, Prop.BORDER_TOP, base);
        b.borderRight = orZero(s, Prop.BORDER_RIGHT, base);
        b.borderBottom = orZero(s, Prop.BORDER_BOTTOM, base);
        b.borderLeft = orZero(s, Prop.BORDER_LEFT, base);
    }
}
