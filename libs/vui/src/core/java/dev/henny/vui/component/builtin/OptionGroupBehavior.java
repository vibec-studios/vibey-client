package dev.henny.vui.component.builtin;

import dev.henny.vui.component.Behavior;
import dev.henny.vui.component.Controls;
import dev.henny.vui.component.Internal;
import dev.henny.vui.dom.Element;
import dev.henny.vui.event.Event;
import dev.henny.vui.event.EventNames;
import dev.henny.vui.event.KeyEvent;
import dev.henny.vui.event.Keys;
import dev.henny.vui.expr.Values;
import dev.henny.vui.style.StateFlags;

import java.util.ArrayList;
import java.util.List;

public final class OptionGroupBehavior implements Behavior {
    private final String childTag;

    public OptionGroupBehavior(String childTag) {
        this.childTag = childTag;
    }

    @Override
    public boolean focusable(Element e) {
        return true;
    }

    @Override
    public void activate(Element e) {
    }

    @Override
    public void mount(Element e) {
        rebuild(e);
    }

    @Override
    public void propChanged(Element e, String name, Object value) {
        if (name.equals("options") && e.flags().mounted) rebuild(e);
        if (name.equals("value")) markSelected(e);
    }

    private void rebuild(Element e) {
        Internal.clear(e, childTag);
        if (hasAuthored(e)) {
            markSelected(e);
            return;
        }
        for (Controls.Option o : Controls.options(e)) {
            Element item = Internal.child(e, childTag);
            item.prop("value", o.value());
            Internal.label(item, o.label());
        }
        markSelected(e);
    }

    private boolean hasAuthored(Element e) {
        for (int i = 0; i < e.childCount(); i++) {
            Element c = e.child(i);
            if (!c.flags().internal && c.tag().equals(childTag)) return true;
        }
        return false;
    }

    private void markSelected(Element e) {
        Object value = Controls.value(e);
        for (Element item : items(e)) item.state(StateFlags.SELECTED, Values.looseEquals(item.prop("value"), value));
    }

    private List<Element> items(Element e) {
        List<Element> out = new ArrayList<>();
        for (int i = 0; i < e.childCount(); i++) if (e.child(i).tag().equals(childTag)) out.add(e.child(i));
        return out;
    }

    @Override
    public void handle(Element e, Event event) {
        if (event.currentTarget() != e) return;
        if (event.type().equals(EventNames.CLICK)) {
            Element item = itemOf(e, event.target());
            if (item != null && !item.disabled()) Controls.commit(e, item.prop("value"));
        } else if (event.type().equals(EventNames.KEY_DOWN) && event instanceof KeyEvent k) {
            int dir = k.key() == Keys.LEFT || k.key() == Keys.UP ? -1 : k.key() == Keys.RIGHT || k.key() == Keys.DOWN ? 1 : 0;
            if (dir == 0) return;
            step(e, dir);
            k.preventDefault();
        }
    }

    private void step(Element e, int dir) {
        List<Element> items = items(e);
        if (items.isEmpty()) return;
        int current = 0;
        for (int i = 0; i < items.size(); i++) if (items.get(i).has(StateFlags.SELECTED)) current = i;
        int next = Math.floorMod(current + dir, items.size());
        Controls.commit(e, items.get(next).prop("value"));
    }

    private Element itemOf(Element group, Element target) {
        for (Element p = target; p != null && p != group; p = p.parent()) if (p.tag().equals(childTag)) return p;
        return null;
    }
}
