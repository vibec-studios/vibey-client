package dev.henny.companio.world;

import dev.henny.companio.def.CompanionDef;
import dev.henny.companio.def.PropDef;
import dev.henny.companio.expr.Scope;

import java.util.HashMap;
import java.util.Map;

final class AutoProps {
    private final Map<String, Boolean> shown = new HashMap<>();
    private final Map<String, Boolean> next = new HashMap<>();

    void update(CompanionDef def, Scope scope) {
        next.clear();
        for (PropDef p : def.props().values()) {
            if (p.when() == null) continue;
            boolean on;
            try {
                on = p.when().test(scope);
            } catch (RuntimeException e) {
                on = false;
            }
            next.put(p.id(), on);
        }
        shown.clear();
        shown.putAll(next);
    }

    Boolean shown(String id) {
        return shown.get(id);
    }
}
