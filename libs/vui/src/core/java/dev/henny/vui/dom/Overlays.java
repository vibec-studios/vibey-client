package dev.henny.vui.dom;

import dev.henny.vui.geom.Placement;
import dev.henny.vui.geom.Rect;
import dev.henny.vui.layout.LayoutEngine;
import dev.henny.vui.layout.ScreenRects;

import java.util.ArrayList;
import java.util.List;

public final class Overlays {
    private final Document document;
    private final List<Element> all = new ArrayList<>();
    private final List<Element> open = new ArrayList<>();
    private Element tooltipBubble;

    Overlays(Document document) {
        this.document = document;
    }

    void register(Element e) {
        e.flags().overlay = true;
        if (!all.contains(e)) all.add(e);
    }

    void forget(Element e) {
        all.remove(e);
        open.remove(e);
    }

    public List<Element> open() {
        return open;
    }

    public List<Element> all() {
        return all;
    }

    public Element tooltipBubble() {
        if (tooltipBubble == null) tooltipBubble = document.createTooltipBubble();
        return tooltipBubble;
    }

    public Element trapScope() {
        for (int i = open.size() - 1; i >= 0; i--) {
            Element e = open.get(i);
            if (e.behavior() instanceof FocusTrap) return e;
        }
        return null;
    }

    public boolean closeTop() {
        for (int i = open.size() - 1; i >= 0; i--) {
            Element e = open.get(i);
            if (e.behavior() instanceof Dismissable d) {
                d.dismiss(e);
                return true;
            }
        }
        return false;
    }

    void refreshOpen() {
        open.clear();
        for (int i = 0; i < all.size(); i++) {
            Element e = all.get(i);
            if (e.flags().overlayOpen && !e.flags().disposed && e.isVisibleInTree()) open.add(e);
        }
    }

    void layout(LayoutEngine engine, float vw, float vh) {
        refreshOpen();
        for (int i = 0; i < open.size(); i++) place(open.get(i), engine, vw, vh);
    }

    private void place(Element e, LayoutEngine engine, float vw, float vh) {
        String placement = stringProp(e, "placement", e.behavior() instanceof OverlayPlacement p ? p.defaultPlacement() : "bottom");
        if (placement.equals("cover")) {
            engine.arrange(e, 0, 0, vw, vh);
            return;
        }
        engine.measure(e, vw, vh);
        float w = e.box().measuredWidth;
        float h = e.box().measuredHeight;
        Element anchor = e.prop("$anchor") instanceof Element a ? a : e.parent();
        Rect anchorRect = anchor == null ? new Rect(vw / 2f, vh / 2f, 0, 0) : ScreenRects.of(anchor);
        if (placement.equals("pointer")) {
            Rect r = Placement.atPoint(document.events().pointerState().x(), document.events().pointerState().y(), w, h, vw, vh, 8);
            engine.arrange(e, r.x(), r.y(), w, h);
            return;
        }
        Rect r = Placement.place(anchorRect, w, h, vw, vh, placement, stringProp(e, "align", "start"), 2);
        engine.arrange(e, r.x(), r.y(), w, h);
    }

    private static String stringProp(Element e, String name, String fallback) {
        Object v = e.prop(name);
        return v == null ? fallback : String.valueOf(v);
    }
}
