package dev.henny.vui.component.builtin;

import dev.henny.vui.component.Behavior;
import dev.henny.vui.component.Controls;
import dev.henny.vui.component.Internal;
import dev.henny.vui.dom.Element;
import dev.henny.vui.event.Event;
import dev.henny.vui.event.EventNames;
import dev.henny.vui.expr.Values;
import dev.henny.vui.layout.LayoutBox;
import dev.henny.vui.paint.PaintContext;
import dev.henny.vui.render.Canvas;
import dev.henny.vui.style.Prop;
import dev.henny.vui.style.StateFlags;

public final class CheckboxBehavior implements Behavior {
    public static final float BOX = 9f;

    @Override
    public boolean focusable(Element e) {
        return true;
    }

    @Override
    public void mount(Element e) {
        e.state(StateFlags.CHECKED, Values.truthy(Controls.value(e)));
        Object label = e.prop("label");
        if (label != null && !Internal.hasAuthoredChildren(e)) Internal.label(e, Values.display(label));
    }

    @Override
    public void propChanged(Element e, String name, Object value) {
        if (name.equals("value")) e.state(StateFlags.CHECKED, Values.truthy(value));
        if (name.equals("label")) {
            Element label = Internal.find(e, "text");
            if (label != null) label.prop("text", Values.display(value));
        }
    }

    @Override
    public void handle(Element e, Event event) {
        if (event.type().equals(EventNames.CLICK) && event.currentTarget() == e) {
            Controls.commit(e, !Values.truthy(Controls.value(e)));
        }
    }

    @Override
    public void paint(Element e, PaintContext ctx) {
        LayoutBox b = e.box();
        Canvas c = ctx.canvas();
        float x = b.x + b.borderLeft;
        float y = b.y + (b.height - BOX) / 2f;
        boolean on = Values.truthy(Controls.value(e));
        int line = e.style().color(Prop.TRACK_COLOR);
        int accent = e.style().color(Prop.ACCENT);
        c.fill(x, y, BOX, BOX, on ? accent : 0xFF08080B);
        int edge = on ? accent : line;
        c.fill(x, y, BOX, 1, edge);
        c.fill(x, y + BOX - 1, BOX, 1, edge);
        c.fill(x, y, 1, BOX, edge);
        c.fill(x + BOX - 1, y, 1, BOX, edge);
        if (!on) return;
        int mark = e.style().color(Prop.THUMB_COLOR);
        c.fill(x + 2, y + 4, 1, 2, mark);
        c.fill(x + 3, y + 5, 1, 2, mark);
        c.fill(x + 4, y + 4, 1, 2, mark);
        c.fill(x + 5, y + 3, 1, 2, mark);
        c.fill(x + 6, y + 2, 1, 2, mark);
    }
}
