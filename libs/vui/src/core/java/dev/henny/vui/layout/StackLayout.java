package dev.henny.vui.layout;

import dev.henny.vui.dom.Element;
import dev.henny.vui.style.ComputedStyle;
import dev.henny.vui.style.Prop;

final class StackLayout {
    private final LayoutEngine engine;

    StackLayout(LayoutEngine engine) {
        this.engine = engine;
    }

    void measure(Element e, float innerMaxW, float innerMaxH, Size out) {
        float w = 0;
        float h = 0;
        for (int i = 0; i < e.childCount(); i++) {
            Element c = e.child(i);
            if (!Flow.inFlow(c)) continue;
            engine.measure(c, innerMaxW, innerMaxH);
            LayoutBox cb = c.box();
            w = Math.max(w, Flow.outerWidth(cb, cb.measuredWidth));
            h = Math.max(h, Flow.outerHeight(cb, cb.measuredHeight));
        }
        out.set(w, h);
    }

    void arrange(Element e) {
        LayoutBox b = e.box();
        float iw = b.innerWidth();
        float ih = b.innerHeight();
        for (int i = 0; i < e.childCount(); i++) {
            Element c = e.child(i);
            if (!Flow.inFlow(c)) continue;
            ComputedStyle cs = c.style();
            LayoutBox cb = c.box();
            Lengths.resolveBox(cb, cs, iw);
            String align = Flow.align(c, e);
            float w = Lengths.resolve(cs, Prop.WIDTH, iw);
            float h = Lengths.resolve(cs, Prop.HEIGHT, ih);
            boolean stretch = align.equals("stretch");
            if (Float.isNaN(w) || Float.isNaN(h)) engine.measure(c, iw, ih);
            if (Float.isNaN(w)) w = stretch ? iw - cb.marginLeft - cb.marginRight : cb.measuredWidth;
            if (Float.isNaN(h)) h = stretch ? ih - cb.marginTop - cb.marginBottom : cb.measuredHeight;
            w = Lengths.clamp(w, Lengths.orZero(cs, Prop.MIN_WIDTH, iw), Lengths.max(cs, Prop.MAX_WIDTH, iw));
            h = Lengths.clamp(h, Lengths.orZero(cs, Prop.MIN_HEIGHT, ih), Lengths.max(cs, Prop.MAX_HEIGHT, ih));
            float x = b.innerX() + cb.marginLeft + offset(align, iw, w + cb.marginLeft + cb.marginRight);
            float y = b.innerY() + cb.marginTop + offset(align, ih, h + cb.marginTop + cb.marginBottom);
            engine.arrange(c, x, y, w, h);
            engine.extend(b, x + w + cb.marginRight, y + h + cb.marginBottom);
        }
    }

    private static float offset(String align, float space, float outer) {
        return switch (align) {
            case "center" -> (space - outer) / 2f;
            case "end" -> space - outer;
            default -> 0f;
        };
    }
}
