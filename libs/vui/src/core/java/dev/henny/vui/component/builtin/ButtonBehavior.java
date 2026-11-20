package dev.henny.vui.component.builtin;

import dev.henny.vui.component.Behavior;
import dev.henny.vui.component.Internal;
import dev.henny.vui.dom.Element;
import dev.henny.vui.expr.Values;

public final class ButtonBehavior implements Behavior {
    @Override
    public boolean focusable(Element e) {
        return true;
    }

    @Override
    public void mount(Element e) {
        Object label = e.prop("label");
        if (label != null && !Internal.hasAuthoredChildren(e)) Internal.label(e, Values.display(label));
    }

    @Override
    public void propChanged(Element e, String name, Object value) {
        if (!name.equals("label")) return;
        Element label = Internal.find(e, "text");
        if (label != null) label.prop("text", Values.display(value));
    }
}
