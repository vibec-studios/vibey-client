package dev.henny.vui.event;

import dev.henny.vui.dom.Element;
import dev.henny.vui.layout.Flow;
import dev.henny.vui.layout.LayoutBox;
import dev.henny.vui.paint.Transforms;
import dev.henny.vui.style.Prop;

import java.util.List;

public final class HitTester {
    private HitTester() {
    }

    public static Element hit(Element root, List<Element> overlays, float x, float y) {
        for (int i = overlays.size() - 1; i >= 0; i--) {
            Element o = overlays.get(i);
            if (!o.flags().overlayOpen || !o.isVisibleInTree()) continue;
            Element h = element(o, x, y, true);
            if (h != null) return h;
        }
        return element(root, x, y, false);
    }

    private static Element element(Element e, float x, float y, boolean overlayRoot) {
        if (!Flow.displayed(e) || e.flags().overlay && !overlayRoot) return null;
        if (e.style().number(Prop.OPACITY) <= 0.001f && !overlayRoot) return null;
        float lx = Transforms.inverseX(e, x, y);
        float ly = Transforms.inverseY(e, x, y);
        LayoutBox b = e.box();
        boolean inside = b.contains(lx, ly);
        if (Flow.clips(e) && !inside) return null;
        if (e.text() == null) {
            boolean scrolls = Flow.scrolls(e);
            float cx = lx + (scrolls ? e.scrollX() : 0);
            float cy = ly + (scrolls ? e.scrollY() : 0);
            for (int i = e.childCount() - 1; i >= 0; i--) {
                Element h = element(e.child(i), cx, cy, false);
                if (h != null) return h;
            }
        }
        if (!inside || e.style().is(Prop.POINTER_EVENTS, "none") || e.style().is(Prop.VISIBILITY, "hidden")) return null;
        return e.tag().equals(Element.TEXT_TAG) ? null : e;
    }
}
