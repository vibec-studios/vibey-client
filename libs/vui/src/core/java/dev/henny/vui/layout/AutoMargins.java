package dev.henny.vui.layout;

import dev.henny.vui.dom.Element;
import dev.henny.vui.style.ComputedStyle;
import dev.henny.vui.style.Prop;

final class AutoMargins {
    private AutoMargins() {
    }

    static boolean mainStart(Element c, boolean row) {
        return c.style().isAuto(row ? Prop.MARGIN_LEFT : Prop.MARGIN_TOP);
    }

    static boolean mainEnd(Element c, boolean row) {
        return c.style().isAuto(row ? Prop.MARGIN_RIGHT : Prop.MARGIN_BOTTOM);
    }

    static int countMain(Element e, int from, int to, boolean row) {
        int n = 0;
        for (int i = from; i < to; i++) {
            Element c = e.child(i);
            if (!Flow.inFlow(c)) continue;
            if (mainStart(c, row)) n++;
            if (mainEnd(c, row)) n++;
        }
        return n;
    }

    static boolean anyCross(Element c, boolean row) {
        ComputedStyle s = c.style();
        return s.isAuto(row ? Prop.MARGIN_TOP : Prop.MARGIN_LEFT) || s.isAuto(row ? Prop.MARGIN_BOTTOM : Prop.MARGIN_RIGHT);
    }

    static String crossAlign(Element c, Element parent, boolean row) {
        ComputedStyle s = c.style();
        boolean start = s.isAuto(row ? Prop.MARGIN_TOP : Prop.MARGIN_LEFT);
        boolean end = s.isAuto(row ? Prop.MARGIN_BOTTOM : Prop.MARGIN_RIGHT);
        if (start && end) return "center";
        if (start) return "end";
        if (end) return "start";
        return Flow.align(c, parent);
    }
}
