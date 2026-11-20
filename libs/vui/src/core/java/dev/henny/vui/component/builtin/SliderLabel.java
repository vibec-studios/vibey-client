package dev.henny.vui.component.builtin;

import dev.henny.vui.component.Controls;
import dev.henny.vui.component.Internal;
import dev.henny.vui.dom.Element;
import dev.henny.vui.expr.Values;
import dev.henny.vui.layout.LayoutBox;
import dev.henny.vui.layout.LayoutEngine;

import java.util.Locale;

final class SliderLabel {
    static final float GAP = 4f;

    private SliderLabel() {
    }

    static boolean wanted(Element e) {
        return Controls.bool(e, "show-value") || Controls.bool(e, "showValue") || e.prop("format") != null
                || e.prop("value-label") != null || e.prop("valueLabel") != null;
    }

    static void sync(Element e) {
        Element label = Internal.find(e, "text");
        if (!wanted(e)) {
            if (label != null) Internal.clear(e, "text");
            return;
        }
        if (label == null) {
            label = Internal.label(e, "");
            label.classes().addFixed("slider-value");
        }
        label.prop("text", text(e, SliderBehavior.current(e)));
    }

    static float reserve(Element e, LayoutEngine layout) {
        Element label = Internal.find(e, "text");
        if (label == null) return 0;
        float w = layout.metrics().width(text(e, SliderBehavior.min(e)));
        w = Math.max(w, layout.metrics().width(text(e, SliderBehavior.max(e))));
        return Math.max(w, layout.metrics().width(text(e, SliderBehavior.current(e))));
    }

    static boolean arrange(Element e, LayoutEngine layout) {
        Element label = Internal.find(e, "text");
        if (label == null) return false;
        LayoutBox b = e.box();
        float w = reserve(e, layout);
        layout.measure(label, w, b.innerHeight());
        float h = label.box().measuredHeight;
        layout.place(label, b.innerX() + b.innerWidth() - w, b.innerY() + (b.innerHeight() - h) / 2f, w, h);
        return true;
    }

    static float trackWidth(Element e, float innerWidth, float reserved) {
        return reserved <= 0 ? innerWidth : Math.max(0, innerWidth - reserved - GAP);
    }

    static String text(Element e, float value) {
        Object explicit = e.prop("value-label") != null ? e.prop("value-label") : e.prop("valueLabel");
        if (explicit != null && value == SliderBehavior.current(e)) return Values.display(explicit);
        String format = Controls.string(e, "format", "{0}");
        if (format.equals("percent")) return Math.round(value * 100) + "%";
        return format.replace("{0}", number(e, value));
    }

    private static String number(Element e, float value) {
        int decimals = (int) Controls.number(e, "decimals", decimalsOf(SliderBehavior.step(e)));
        if (decimals <= 0) return Long.toString(Math.round(value));
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }

    private static int decimalsOf(float step) {
        for (int d = 0; d < 4; d++) {
            double scaled = step * Math.pow(10, d);
            if (Math.abs(scaled - Math.rint(scaled)) < 1e-4) return d;
        }
        return 2;
    }
}
