package dev.henny.vui.event;

import dev.henny.vui.dom.Element;
import dev.henny.vui.style.StateFlags;

final class Hover {
    private Hover() {
    }

    static void update(PointerState state, Element target, EventSystem events) {
        Element old = state.hovered;
        if (old == target) return;
        state.hovered = target;
        for (Element e = old; e != null; e = e.parent()) {
            if (target != null && isAncestorOrSelf(e, target)) break;
            e.state(StateFlags.HOVER, false);
            events.signal(e, EventNames.POINTER_LEAVE);
        }
        for (Element e = target; e != null; e = e.parent()) {
            if (e.has(StateFlags.HOVER)) break;
            e.state(StateFlags.HOVER, true);
            events.signal(e, EventNames.POINTER_ENTER);
        }
        events.tooltips().hovered(target);
    }

    static void active(Element target, boolean on) {
        Element blocked = null;
        if (on) for (Element e = target; e != null; e = e.parent()) if (e.disabled()) blocked = e;
        boolean skip = blocked != null;
        for (Element e = target; e != null; e = e.parent()) {
            if (!skip || !on) e.state(StateFlags.ACTIVE, on);
            if (e == blocked) skip = false;
        }
    }

    static boolean isAncestorOrSelf(Element ancestor, Element node) {
        for (Element e = node; e != null; e = e.parent()) if (e == ancestor) return true;
        return false;
    }
}
