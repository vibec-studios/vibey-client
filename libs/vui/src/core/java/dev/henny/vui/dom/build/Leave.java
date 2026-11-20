package dev.henny.vui.dom.build;

import dev.henny.vui.dom.ChildSource;
import dev.henny.vui.dom.Document;
import dev.henny.vui.dom.Element;
import dev.henny.vui.dom.Lifecycle;
import dev.henny.vui.style.StateFlags;

import java.util.ArrayList;
import java.util.List;

public final class Leave {
    private Leave() {
    }

    public static boolean begin(Element e, ChildSource owner) {
        Document doc = e.document();
        if (doc == null || !doc.isMounted() || !doc.hasLeaveRules() || e.flags().leaving) {
            Lifecycle.dispose(e);
            return false;
        }
        e.flags().leaving = true;
        e.state(StateFlags.LEAVE, true);
        doc.leaving(e, owner);
        return true;
    }

    public static List<Element> collectAll(List<ChildSource> sources) {
        List<Element> out = new ArrayList<>();
        for (ChildSource s : sources) s.collect(out);
        return out;
    }
}
