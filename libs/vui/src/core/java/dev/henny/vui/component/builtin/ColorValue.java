package dev.henny.vui.component.builtin;

import dev.henny.vui.expr.Values;
import dev.henny.vui.style.ColorParser;
import dev.henny.vui.style.StyleError;

final class ColorValue {
    private ColorValue() {
    }

    static int parse(Object value) {
        if (value instanceof Number n) return (int) n.longValue();
        if (value == null) return 0xFFFFFFFF;
        try {
            return ColorParser.parse(Values.display(value));
        } catch (StyleError | NumberFormatException e) {
            return 0xFFFFFFFF;
        }
    }

    static Object format(int argb, Object like) {
        if (like instanceof Number) return (double) (argb & 0xFFFFFFFFL);
        if ((argb >>> 24) == 0xFF) return String.format("#%06x", argb & 0xFFFFFF);
        return ColorParser.format(argb);
    }
}
