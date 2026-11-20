package dev.henny.vui.dom;

import dev.henny.vui.expr.Values;
import dev.henny.vui.style.StateFlags;

import java.util.Set;

final class ElementProps {
    private static final Set<String> LAYOUT_PROPS = Set.of("text", "label", "src", "options", "tabs", "items",
            "placeholder", "icon", "glyph", "size", "item-size", "lines");

    private ElementProps() {
    }

    static void applied(Element e, String name, Object value) {
        switch (name) {
            case "disabled" -> e.state(StateFlags.DISABLED, Values.truthy(value));
            case "checked" -> e.state(StateFlags.CHECKED, Values.truthy(value));
            case "selected" -> e.state(StateFlags.SELECTED, Values.truthy(value));
            case "invalid" -> e.state(StateFlags.INVALID, Values.truthy(value));
            default -> {
            }
        }
        if (e.behavior() != null) e.behavior().propChanged(e, name, value);
        if (LAYOUT_PROPS.contains(name)) {
            invalidateText(e);
            e.markLayoutDirty();
        }
    }

    private static void invalidateText(Element e) {
        for (Element p = e; p != null; p = p.parent()) {
            if (p.text() != null) {
                p.text().invalidate();
                return;
            }
        }
    }
}
