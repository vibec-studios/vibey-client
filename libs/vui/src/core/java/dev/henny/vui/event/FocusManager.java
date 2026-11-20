package dev.henny.vui.event;

import dev.henny.vui.dom.Document;
import dev.henny.vui.dom.Element;
import dev.henny.vui.layout.Flow;
import dev.henny.vui.style.StateFlags;

import java.util.ArrayList;
import java.util.List;

public final class FocusManager {
    private final Document document;
    private Element focused;
    private boolean visible;

    public FocusManager(Document document) {
        this.document = document;
    }

    public Element focused() {
        return focused;
    }

    public boolean visible() {
        return visible;
    }

    public void focus(Element next, boolean byKeyboard) {
        if (next != null && (!focusable(next) || !next.isVisibleInTree())) return;
        visible = byKeyboard;
        if (next == focused) return;
        Element old = focused;
        focused = next;
        if (old != null) {
            old.state(StateFlags.FOCUS, false);
            for (Element p = old.parent(); p != null; p = p.parent()) p.state(StateFlags.FOCUS_WITHIN, false);
            document.events().signal(old, EventNames.BLUR);
        }
        if (next != null) {
            next.state(StateFlags.FOCUS, true);
            for (Element p = next.parent(); p != null; p = p.parent()) p.state(StateFlags.FOCUS_WITHIN, true);
            document.events().signal(next, EventNames.FOCUS);
        }
    }

    public void blur() {
        focus(null, false);
    }

    public void forget(Element e) {
        if (focused == e) {
            focused = null;
            visible = false;
        }
    }

    public boolean move(boolean forward) {
        List<Element> all = focusables();
        if (all.isEmpty()) return false;
        int index = all.indexOf(focused);
        int next = index < 0 ? (forward ? 0 : all.size() - 1) : Math.floorMod(index + (forward ? 1 : -1), all.size());
        focus(all.get(next), true);
        return true;
    }

    public List<Element> focusables() {
        List<Element> out = new ArrayList<>();
        Element scope = document.overlays().trapScope();
        collect(scope != null ? scope : document.root(), out, scope != null);
        return out;
    }

    public static Element focusableAncestor(Element e) {
        for (Element p = e; p != null; p = p.parent()) if (focusable(p)) return p;
        return null;
    }

    public static boolean focusable(Element e) {
        if (e.disabled()) return false;
        Object tab = e.prop("tabindex");
        if (tab != null) return dev.henny.vui.expr.Values.toNumber(tab) >= 0;
        return e.behavior().focusable(e);
    }

    private void collect(Element e, List<Element> out, boolean insideTrap) {
        if (!Flow.displayed(e) || e.disabled()) return;
        if (e.flags().overlay && !e.flags().overlayOpen) return;
        if (e.flags().overlay && !insideTrap && document.overlays().trapScope() != null && e != document.overlays().trapScope()) return;
        if (focusable(e)) out.add(e);
        if (e.text() != null) return;
        for (int i = 0; i < e.childCount(); i++) collect(e.child(i), out, insideTrap);
    }
}
