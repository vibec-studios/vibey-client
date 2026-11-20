package dev.henny.vui.dom.build;

import dev.henny.vui.expr.Scope;
import dev.henny.vui.template.CElement;
import dev.henny.vui.template.CNode;
import dev.henny.vui.template.SlotSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Slots {
    private Slots() {
    }

    public static Map<String, SlotContent> collect(CElement usage, Scope scope, BuildContext ctx) {
        Map<String, SlotContent> out = new HashMap<>();
        List<CNode> defaults = new ArrayList<>();
        for (CNode n : usage.children()) {
            if (n instanceof CElement e && e.isTemplate() && e.slot() != null) {
                out.put(e.slot().name(), new SlotContent(e.children(), scope, ctx, e.slot()));
            } else {
                defaults.add(n);
            }
        }
        if (!defaults.isEmpty() && !out.containsKey("default")) {
            SlotSpec spec = usage.slot() != null ? usage.slot() : new SlotSpec("default", new String[0], null);
            out.put("default", new SlotContent(defaults, scope, ctx, spec));
        }
        return out;
    }
}
