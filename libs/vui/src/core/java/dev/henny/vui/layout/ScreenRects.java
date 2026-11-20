package dev.henny.vui.layout;

import dev.henny.vui.dom.Element;
import dev.henny.vui.geom.Rect;

public final class ScreenRects {
    private ScreenRects() {
    }

    public static Rect of(Element e) {
        LayoutBox b = e.box();
        float x = b.x;
        float y = b.y;
        for (Element p = e.parent(); p != null; p = p.parent()) {
            if (p.flags().overlay) break;
            if (Flow.scrolls(p)) {
                x -= p.scrollX();
                y -= p.scrollY();
            }
        }
        return new Rect(x, y, b.width, b.height);
    }
}
