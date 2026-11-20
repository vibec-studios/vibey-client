package dev.henny.vui.dom;

import dev.henny.vui.template.CompiledTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ComponentSheets {
    private final Map<String, CompiledTemplate> used = new LinkedHashMap<>();

    boolean add(CompiledTemplate t) {
        if (used.containsKey(t.id())) return false;
        used.put(t.id(), t);
        return true;
    }

    void clear() {
        used.clear();
    }

    List<CompiledTemplate> importsFirst() {
        List<CompiledTemplate> out = new ArrayList<>(used.size());
        Set<String> done = new HashSet<>();
        for (CompiledTemplate t : used.values()) visit(t, done, new HashSet<>(), out);
        return out;
    }

    private void visit(CompiledTemplate t, Set<String> done, Set<String> path, List<CompiledTemplate> out) {
        if (done.contains(t.id()) || !path.add(t.id())) return;
        for (String imported : t.components().values()) {
            CompiledTemplate dep = used.get(imported);
            if (dep != null) visit(dep, done, path, out);
        }
        done.add(t.id());
        out.add(t);
    }
}
