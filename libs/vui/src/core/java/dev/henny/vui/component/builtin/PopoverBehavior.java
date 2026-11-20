package dev.henny.vui.component.builtin;

import dev.henny.vui.dom.Element;
import dev.henny.vui.expr.Values;

public final class PopoverBehavior extends OverlayBehavior {
    public PopoverBehavior() {
        super("bottom", true);
    }

    @Override
    public void propChanged(Element e, String name, Object value) {
        if (name.equals("open")) open(e, Values.truthy(value));
    }

    @Override
    public void mount(Element e) {
        open(e, Values.truthy(e.prop("open")));
    }

    @Override
    public String modelProp() {
        return "open";
    }
}
