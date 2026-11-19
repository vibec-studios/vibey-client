package dev.henny.vui.component.builtin;

import dev.henny.vui.component.Behavior;
import dev.henny.vui.component.Controls;
import dev.henny.vui.component.Internal;
import dev.henny.vui.dom.Element;
import dev.henny.vui.event.Event;
import dev.henny.vui.event.EventNames;
import dev.henny.vui.layout.LayoutBox;
import dev.henny.vui.paint.PaintContext;

public final class ColorPickerBehavior implements Behavior {
    @Override
    public boolean focusable(Element e) {
        return true;
    }

    @Override
    public void mount(Element e) {
        Internal.child(e, "colorpicker-panel");
    }

    @Override
    public void activate(Element e) {
        Element panel = Internal.find(e, "colorpicker-panel");
        if (panel == null) return;
        boolean open = !panel.flags().overlayOpen;
        if (open) panel.behaviorState(Hsv.fromRgb(ColorValue.parse(Controls.value(e))));
        OverlayBehavior.open(panel, open);
    }

    @Override
    public void handle(Element e, Event event) {
        if (event.currentTarget() != e || !event.type().equals(EventNames.CLICK)) return;
        for (Element p = event.target(); p != null && p != e; p = p.parent()) if (p.tag().equals("colorpicker-panel")) return;
        activate(e);
    }

    @Override
    public void paint(Element e, PaintContext ctx) {
        LayoutBox b = e.box();
        ctx.canvas().fill(b.innerX(), b.innerY(), b.innerWidth(), b.innerHeight(), ColorValue.parse(Controls.value(e)));
    }
}
