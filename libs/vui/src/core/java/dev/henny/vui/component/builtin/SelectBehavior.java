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
import dev.henny.vui.layout.LayoutBox;
import dev.henny.vui.paint.PaintContext;
import dev.henny.vui.style.Prop;
import dev.henny.vui.style.StateFlags;

import java.util.List;

public final class SelectBehavior implements Behavior {
    @Override
    public boolean focusable(Element e) {
        return true;
    }

    @Override
    public void mount(Element e) {
        Internal.label(e, "");
        Internal.child(e, "select-menu");
        rebuildOptions(e);
    }

    @Override
    public void propChanged(Element e, String name, Object value) {
        if (!e.flags().mounted) return;
        if (name.equals("options")) rebuildOptions(e);
        if (name.equals("value")) refreshSelection(e);
    }

    @Override
    public void activate(Element e) {
        Element menu = menu(e);
        if (menu != null) OverlayBehavior.open(menu, !menu.flags().overlayOpen);
    }

    @Override
    public void handle(Element e, Event event) {
        if (event.currentTarget() != e) return;
        if (event.type().equals(EventNames.CLICK)) {
            Element option = optionOf(e, event.target());
            if (option != null) {
                Controls.commit(e, option.prop("value"));
                OverlayBehavior.open(menu(e), false);
            } else if (!insideMenu(e, event.target())) {
                activate(e);
            }
        } else if (event.type().equals(EventNames.KEY_DOWN) && event instanceof KeyEvent k) {
            int dir = k.key() == Keys.UP ? -1 : k.key() == Keys.DOWN ? 1 : 0;
            if (dir == 0) return;
            List<Controls.Option> options = Controls.options(e);
            if (options.isEmpty()) return;
            int index = Controls.selectedIndex(e, options);
            Controls.commit(e, options.get(Math.floorMod(index + dir, options.size())).value());
            k.preventDefault();
        }
    }

    @Override
    public void paint(Element e, PaintContext ctx) {
        LayoutBox b = e.box();
        float x = b.x + b.width - b.borderRight - b.paddingRight - 5;
        float y = b.y + b.height / 2f - 1;
        int color = e.style().color(Prop.COLOR);
        ctx.canvas().fill(x, y, 5, 1, color);
        ctx.canvas().fill(x + 1, y + 1, 3, 1, color);
        ctx.canvas().fill(x + 2, y + 2, 1, 1, color);
    }

    private void rebuildOptions(Element e) {
        Element menu = menu(e);
        if (menu == null) return;
        Internal.clear(menu, "select-option");
        for (Controls.Option o : Controls.options(e)) {
            Element item = Internal.child(menu, "select-option");
            item.prop("value", o.value());
            Internal.label(item, o.label());
        }
        refreshSelection(e);
    }

    private void refreshSelection(Element e) {
        Element label = Internal.find(e, "text");
        List<Controls.Option> options = Controls.options(e);
        int index = Controls.selectedIndex(e, options);
        String text = index >= 0 ? options.get(index).label() : Controls.string(e, "placeholder", "—");
        if (label != null) label.prop("text", text);
        Element menu = menu(e);
        if (menu == null) return;
        Object value = Controls.value(e);
        for (int i = 0; i < menu.childCount(); i++) {
            Element item = menu.child(i);
            item.state(StateFlags.SELECTED, Values.looseEquals(item.prop("value"), value));
        }
    }

    private static Element menu(Element e) {
        return Internal.find(e, "select-menu");
    }

    private static Element optionOf(Element select, Element target) {
        for (Element p = target; p != null && p != select; p = p.parent()) if (p.tag().equals("select-option")) return p;
        return null;
    }

    private static boolean insideMenu(Element select, Element target) {
        for (Element p = target; p != null && p != select; p = p.parent()) if (p.tag().equals("select-menu")) return true;
        return false;
    }
}
