package dev.henny.vui.event;

import dev.henny.vui.dom.Document;
import dev.henny.vui.dom.Element;
import dev.henny.vui.expr.Values;

public final class TooltipController {
    public static final float DELAY = 0.45f;

    private final Document document;
    private Element owner;
    private Element shownElement;
    private float hoverTime;

    public TooltipController(Document document) {
        this.document = document;
    }

    public Element owner() {
        return owner;
    }

    public boolean showing() {
        return shownElement != null;
    }

    public void hovered(Element target) {
        Element next = find(target);
        if (next == owner) return;
        hide();
        owner = next;
        hoverTime = 0;
    }

    public void frame(float dt) {
        if (owner == null || shownElement != null) return;
        hoverTime += dt;
        if (hoverTime >= DELAY) show();
    }

    public void show() {
        if (owner == null) return;
        Element child = childTooltip(owner);
        if (child != null) {
            child.flags().overlayOpen = true;
            shownElement = child;
        } else {
            Element bubble = document.overlays().tooltipBubble();
            bubble.prop("$anchor", owner);
            setText(bubble, Values.display(owner.prop("tooltip")));
            bubble.flags().overlayOpen = true;
            shownElement = bubble;
        }
        document.invalidateLayout();
    }

    public void hide() {
        if (shownElement != null) {
            shownElement.flags().overlayOpen = false;
            shownElement = null;
            document.invalidateLayout();
        }
        hoverTime = 0;
    }

    public void reset() {
        hide();
        owner = null;
    }

    public void forget(Element e) {
        if (owner == e || shownElement == e) reset();
    }

    private static Element find(Element target) {
        for (Element p = target; p != null; p = p.parent()) {
            Object tip = p.prop("tooltip");
            if (tip != null && !Values.display(tip).isEmpty()) return p;
            if (childTooltip(p) != null) return p;
        }
        return null;
    }

    private static Element childTooltip(Element e) {
        for (int i = 0; i < e.childCount(); i++) if (e.child(i).tag().equals("tooltip")) return e.child(i);
        return null;
    }

    private static void setText(Element bubble, String text) {
        if (bubble.childCount() == 0) return;
        Element label = bubble.child(0);
        label.prop("text", text);
    }
}
