package dev.henny.vui.layout;

import dev.henny.vui.dom.Element;
import dev.henny.vui.style.ComputedStyle;
import dev.henny.vui.style.Prop;

final class PositionedLayout {
    private final LayoutEngine engine;

    PositionedLayout(LayoutEngine engine) {
        this.engine = engine;
    }

    void arrangeAll(Element parent) {
        for (int i = 0; i < parent.childCount(); i++) {
            Element c = parent.child(i);
            if (Flow.positioned(c)) arrange(c, parent);
        }
    }

    private void arrange(Element c, Element parent) {
        ComputedStyle s = c.style();
        LayoutBox pb = parent.box();
        boolean fixed = s.is(Prop.POSITION, "fixed");
        float rx = fixed ? 0 : pb.x + pb.borderLeft;
        float ry = fixed ? 0 : pb.y + pb.borderTop;
        float rw = fixed ? engine.viewportWidth() : pb.width - pb.borderLeft - pb.borderRight;
        float rh = fixed ? engine.viewportHeight() : pb.height - pb.borderTop - pb.borderBottom;
        float left = Lengths.resolve(s, Prop.LEFT, rw);
        float right = Lengths.resolve(s, Prop.RIGHT, rw);
        float top = Lengths.resolve(s, Prop.TOP, rh);
        float bottom = Lengths.resolve(s, Prop.BOTTOM, rh);
        float w = Lengths.resolve(s, Prop.WIDTH, rw);
        float h = Lengths.resolve(s, Prop.HEIGHT, rh);
        if (Float.isNaN(w) && !Float.isNaN(left) && !Float.isNaN(right)) w = rw - left - right;
        if (Float.isNaN(h) && !Float.isNaN(top) && !Float.isNaN(bottom)) h = rh - top - bottom;
        if (Float.isNaN(w) || Float.isNaN(h)) {
            engine.measure(c, Float.isNaN(w) ? rw : w, Float.isNaN(h) ? rh : h);
            if (Float.isNaN(w)) w = c.box().measuredWidth;
            if (Float.isNaN(h)) h = c.box().measuredHeight;
        }
        Anchor anchor = Anchor.fromCss(s.keyword(Prop.ANCHOR));
        float x;
        float y;
        if (anchor != null) {
            float dx = (Float.isNaN(left) ? 0 : left) - (Float.isNaN(right) ? 0 : right);
            float dy = (Float.isNaN(top) ? 0 : top) - (Float.isNaN(bottom) ? 0 : bottom);
            x = anchor.x(rx, rw, w, dx);
            y = anchor.y(ry, rh, h, dy);
        } else {
            x = !Float.isNaN(left) ? rx + left : !Float.isNaN(right) ? rx + rw - right - w : rx;
            y = !Float.isNaN(top) ? ry + top : !Float.isNaN(bottom) ? ry + rh - bottom - h : ry;
        }
        engine.arrange(c, x, y, w, h);
    }
}
