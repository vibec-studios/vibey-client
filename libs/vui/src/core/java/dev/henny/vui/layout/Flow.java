package dev.henny.vui.layout;

import dev.henny.vui.dom.Element;
import dev.henny.vui.style.ComputedStyle;
import dev.henny.vui.style.Prop;

public final class Flow {
    private Flow() {
    }

    public static boolean displayed(Element e) {
        return e.flags().shown && !e.style().is(Prop.DISPLAY, "none");
    }

    public static boolean inFlow(Element e) {
        if (!displayed(e) || e.flags().overlay) return false;
        ComputedStyle s = e.style();
        return !s.is(Prop.POSITION, "absolute") && !s.is(Prop.POSITION, "fixed");
    }

    public static boolean positioned(Element e) {
        if (!displayed(e) || e.flags().overlay) return false;
        ComputedStyle s = e.style();
        return s.is(Prop.POSITION, "absolute") || s.is(Prop.POSITION, "fixed");
    }

    public static boolean isRow(Element e) {
        return e.style().is(Prop.DISPLAY, "row");
    }

    public static boolean scrolls(Element e) {
        return e.style().is(Prop.OVERFLOW, "scroll");
    }

    public static boolean clips(Element e) {
        return !e.style().is(Prop.OVERFLOW, "visible");
    }

    public static String align(Element child, Element parent) {
        String self = child.style().keyword(Prop.ALIGN_SELF);
        return self.equals("auto") || self.isEmpty() ? parent.style().keyword(Prop.ALIGN_ITEMS) : self;
    }

    public static float outerWidth(LayoutBox b, float width) {
        return width + b.marginLeft + b.marginRight;
    }

    public static float outerHeight(LayoutBox b, float height) {
        return height + b.marginTop + b.marginBottom;
    }
}
