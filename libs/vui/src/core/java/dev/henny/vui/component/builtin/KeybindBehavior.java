package dev.henny.vui.component.builtin;

import dev.henny.vui.component.Behavior;
import dev.henny.vui.component.Controls;
import dev.henny.vui.component.Internal;
import dev.henny.vui.dom.Element;
import dev.henny.vui.event.Event;
import dev.henny.vui.event.EventNames;
import dev.henny.vui.event.KeyEvent;
import dev.henny.vui.event.Keys;
import dev.henny.vui.render.Input;
import dev.henny.vui.style.StateFlags;

public final class KeybindBehavior implements Behavior {
    public static final String WAITING_LABEL = "…";

    @Override
    public boolean focusable(Element e) {
        return true;
    }

    @Override
    public void mount(Element e) {
        Internal.label(e, "");
        refresh(e);
    }

    @Override
    public void propChanged(Element e, String name, Object value) {
        if (name.equals("value") && e.flags().mounted) refresh(e);
    }

    @Override
    public void activate(Element e) {
        waiting(e, !e.has(StateFlags.OPEN));
    }

    @Override
    public void handle(Element e, Event event) {
        if (event.currentTarget() != e) return;
        if (event.type().equals(EventNames.CLICK)) {
            activate(e);
            return;
        }
        if (event.type().equals(EventNames.BLUR)) {
            waiting(e, false);
            return;
        }
        if (!event.type().equals(EventNames.KEY_DOWN) || !e.has(StateFlags.OPEN)) return;
        KeyEvent k = (KeyEvent) event;
        k.preventDefault();
        k.stopPropagation();
        if (k.key() == Keys.ESCAPE) {
            waiting(e, false);
            return;
        }
        int code = k.key() == Keys.BACKSPACE || k.key() == Keys.DELETE ? Keys.NONE : k.key();
        waiting(e, false);
        Controls.commit(e, (double) code);
        e.prop("scancode", (double) k.scancode());
        refresh(e);
    }

    private void waiting(Element e, boolean on) {
        e.state(StateFlags.OPEN, on);
        refresh(e);
    }

    private void refresh(Element e) {
        Element label = Internal.find(e, "text");
        if (label == null) return;
        label.prop("text", e.has(StateFlags.OPEN) ? WAITING_LABEL : name(e));
    }

    static String name(Element e) {
        int key = (int) Controls.number(e, "value", Keys.NONE);
        if (key == Keys.NONE) return Keys.fallbackName(key);
        Input input = e.document() == null ? null : e.document().vui().backend().input();
        String n = input == null ? null : input.keyName(key, (int) Controls.number(e, "scancode", 0));
        return n == null || n.isEmpty() ? Keys.fallbackName(key) : n;
    }
}
