package dev.henny.vui.component.builtin;

import dev.henny.vui.component.Behavior;
import dev.henny.vui.dom.Element;
import dev.henny.vui.event.Event;
import dev.henny.vui.event.EventNames;
import dev.henny.vui.layout.LayoutBox;
import dev.henny.vui.paint.PaintContext;

public final class ColorSwatchBehavior implements Behavior {
    @Override
    public void activate(Element e) {
    }

    @Override
    public void handle(Element e, Event event) {
        if (event.currentTarget() != e || !event.type().equals(EventNames.CLICK)) return;
        Element picker = ColorPickerParts.picker(e);
        if (picker != null) ColorPickerParts.choose(picker, e.prop("value"));
        event.stopPropagation();
    }

    @Override
    public void paint(Element e, PaintContext ctx) {
        LayoutBox b = e.box();
        ctx.canvas().fill(b.innerX(), b.innerY(), b.innerWidth(), b.innerHeight(), ColorValue.parse(e.prop("value")));
    }
}
