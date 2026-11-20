package dev.henny.vui.event;

import dev.henny.vui.dom.Element;

final class Clicks {
    private Clicks() {
    }

    static void release(PointerState state, EventSystem events, Element pressed, Element released, float x, float y,
                        int button, int mods, float now) {
        Element target = Hover.isAncestorOrSelf(pressed, released) ? released
                : Hover.isAncestorOrSelf(released, pressed) ? pressed : null;
        if (target == null) return;
        if (button == PointerEvent.RIGHT) {
            events.send(new PointerEvent().pointer(EventNames.CONTEXT_MENU, target, x, y, button, mods), true);
            return;
        }
        if (button != PointerEvent.LEFT) return;
        boolean second = state.lastClickTarget == target && now - state.lastClickTime <= PointerState.DOUBLE_CLICK_SECONDS;
        int clicks = second ? 2 : 1;
        events.send(new PointerEvent().pointer(EventNames.CLICK, target, x, y, button, mods).clicks(clicks), true);
        if (second) {
            events.send(new PointerEvent().pointer(EventNames.DOUBLE_CLICK, target, x, y, button, mods).clicks(2), true);
            state.lastClickTarget = null;
        } else {
            state.lastClickTarget = target;
            state.lastClickTime = now;
        }
    }
}
