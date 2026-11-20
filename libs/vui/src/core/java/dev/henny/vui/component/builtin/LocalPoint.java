package dev.henny.vui.component.builtin;

import dev.henny.vui.dom.Element;
import dev.henny.vui.geom.Rect;
import dev.henny.vui.layout.ScreenRects;

final class LocalPoint {
    private LocalPoint() {
    }

    static float fractionX(Element e, float screenX) {
        Rect r = ScreenRects.of(e);
        float inset = e.box().borderLeft + e.box().paddingLeft;
        float width = e.box().innerWidth();
        if (width <= 0) return 0f;
        return Math.max(0f, Math.min(1f, (screenX - r.x() - inset) / width));
    }

    static float fractionY(Element e, float screenY) {
        Rect r = ScreenRects.of(e);
        float inset = e.box().borderTop + e.box().paddingTop;
        float height = e.box().innerHeight();
        if (height <= 0) return 0f;
        return Math.max(0f, Math.min(1f, (screenY - r.y() - inset) / height));
    }

    static float x(Element e, float screenX) {
        return screenX - ScreenRects.of(e).x();
    }
}
