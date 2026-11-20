package dev.henny.vui.component.builtin;

import dev.henny.vui.dom.Element;
import dev.henny.vui.expr.Values;
import dev.henny.vui.icon.IconRegistry;
import dev.henny.vui.icon.PixelIcon;

final class IconLookup {
    private IconLookup() {
    }

    static PixelIcon icon(Element e) {
        Object name = e.prop("name");
        if (name == null || e.document() == null) return null;
        IconRegistry icons = e.document().vui().icons();
        Cached c = e.behaviorState();
        if (c != null && c.version == icons.version() && c.name == name) return c.icon;
        String ref = Values.display(name);
        Object set = e.prop("set");
        PixelIcon icon = ref.isEmpty() ? null : icons.find(ref, set == null ? null : Values.display(set));
        e.behaviorState(new Cached(icons.version(), name, icon));
        return icon;
    }

    static void check(Element e) {
        Object name = e.prop("name");
        if (name == null || e.document() == null || Values.display(name).isEmpty()) return;
        if (icon(e) != null) return;
        String ref = Values.display(name);
        String hint = e.document().vui().icons().suggest(ref);
        e.document().errors().report(e.pos(), "unknown icon '" + ref + "'",
                hint != null ? "did you mean '" + hint + "'?" : "add it to assets/<ns>/vui/icons.json and use name=\"ns:icon\"");
    }

    private record Cached(int version, Object name, PixelIcon icon) {
    }
}
