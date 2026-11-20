package dev.henny.vui.component.builtin;

import dev.henny.vui.dom.Element;
import dev.henny.vui.dom.FocusTrap;
import dev.henny.vui.event.Event;
import dev.henny.vui.event.EventNames;
import dev.henny.vui.event.FocusManager;
import dev.henny.vui.expr.Values;

import java.util.List;

public final class ModalBehavior extends OverlayBehavior implements FocusTrap {
    public ModalBehavior() {
        super("cover", false);
    }

    @Override
    public void mount(Element e) {
        setOpen(e, Values.truthy(e.prop("open")));
    }

    @Override
    public void propChanged(Element e, String name, Object value) {
        if (name.equals("open")) setOpen(e, Values.truthy(value));
    }

    @Override
    public String modelProp() {
        return "open";
    }

    @Override
    public void handle(Element e, Event event) {
        if (event.type().equals(EventNames.CLICK) && event.target() == e && !Values.truthy(e.prop("persistent"))) dismiss(e);
    }

    private static void setOpen(Element e, boolean open) {
        boolean was = e.flags().overlayOpen;
        open(e, open);
        if (!open || was || e.document() == null) return;
        FocusManager focus = e.document().events().focus();
        List<Element> inside = focus.focusables();
        for (Element candidate : inside) {
            if (isInside(candidate, e)) {
                focus.focus(candidate, true);
                return;
            }
        }
    }

    private static boolean isInside(Element node, Element ancestor) {
        for (Element p = node; p != null; p = p.parent()) if (p == ancestor) return true;
        return false;
    }
}
