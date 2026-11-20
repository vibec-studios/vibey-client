package dev.henny.vui.event;

import dev.henny.vui.dom.Document;
import dev.henny.vui.dom.Element;

final class Keyboard {
    private final Document document;
    private final EventSystem events;

    Keyboard(Document document, EventSystem events) {
        this.document = document;
        this.events = events;
    }

    boolean down(KeyEvent e, int key, int scancode, int mods, boolean repeat) {
        Element focused = events.focus().focused();
        Element target = focused != null ? focused : document.root();
        e.key(EventNames.KEY_DOWN, target, key, scancode, mods, repeat);
        boolean handled = events.send(e, true);
        if (e.prevented()) return true;
        if (key == Keys.TAB) return events.focus().move((mods & Mods.SHIFT) == 0);
        if ((Keys.isEnter(key) || key == Keys.SPACE) && focused != null && !focused.disabled()) {
            focused.behavior().activate(focused);
            return true;
        }
        if (key == Keys.ESCAPE) return escape(target);
        return handled;
    }

    private boolean escape(Element target) {
        Event esc = new Event().reset(EventNames.ESCAPE, target, true, null);
        events.send(esc, false);
        if (esc.prevented() || esc.stopped()) return true;
        if (document.overlays().closeTop()) return true;
        if (events.focus().focused() != null && events.focus().visible()) {
            events.focus().blur();
            return true;
        }
        return document.requestEscape();
    }
}
