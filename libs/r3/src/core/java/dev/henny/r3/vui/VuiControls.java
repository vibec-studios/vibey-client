package dev.henny.r3.vui;

import dev.henny.vui.dom.Element;
import dev.henny.vui.event.EventNames;
import dev.henny.vui.event.Handler;

import java.util.List;

public final class VuiControls {
    public static final String ANONYMOUS_PREFIX = "vui@";

    private VuiControls() {}

    public static String controlId(Element hit) {
        Element e = control(hit);
        if (e == null) return null;
        String id = e.id();
        return id != null && !id.isEmpty() ? id : ANONYMOUS_PREFIX + e.pos();
    }

    public static Element control(Element hit) {
        for (Element e = hit; e != null; e = e.parent()) {
            if (e.disabled()) return null;
            String id = e.id();
            if (id != null && !id.isEmpty()) return e;
            if (hasClick(e)) return e;
        }
        return null;
    }

    public static boolean hasClick(Element e) {
        List<Handler> handlers = e.handlers();
        for (int i = 0; i < handlers.size(); i++) {
            if (EventNames.CLICK.equals(handlers.get(i).type())) return true;
        }
        return false;
    }
}
