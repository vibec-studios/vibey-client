package dev.henny.vui.event;

import dev.henny.vui.dom.Element;
import dev.henny.vui.layout.Flow;
import dev.henny.vui.layout.LayoutBox;

final class WheelScroll {
    static final float STEP = 18f;

    private WheelScroll() {
    }

    static boolean apply(Element target, float dx, float dy) {
        for (Element e = target; e != null; e = e.parent()) {
            if (!Flow.scrolls(e)) continue;
            LayoutBox b = e.box();
            boolean canY = dy != 0 && b.contentHeight > b.innerHeight() + 0.5f
                    && (dy > 0 ? e.scrollY() > 0 : e.scrollY() < b.contentHeight - b.innerHeight());
            boolean canX = dx != 0 && b.contentWidth > b.innerWidth() + 0.5f
                    && (dx > 0 ? e.scrollX() > 0 : e.scrollX() < b.contentWidth - b.innerWidth());
            if (!canX && !canY) continue;
            e.scrollTo(e.scrollX() - dx * STEP, e.scrollY() - dy * STEP);
            return true;
        }
        return false;
    }
}
