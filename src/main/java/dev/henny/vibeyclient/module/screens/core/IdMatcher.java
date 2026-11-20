package dev.henny.vibeyclient.module.screens.core;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class IdMatcher {
    private IdMatcher() {}

    public static String[] match(List<WidgetIds.Id> ids, Map<String, Placement> saved) {
        String[] out = new String[ids.size()];
        Set<String> used = new HashSet<>();
        for (int i = 0; i < ids.size(); i++) {
            String p = ids.get(i).primary();
            if (saved.containsKey(p)) {
                out[i] = p;
                used.add(p);
            }
        }
        for (Map.Entry<String, Placement> e : saved.entrySet()) {
            if (used.contains(e.getKey()) || e.getValue().fallback() == null) continue;
            for (int i = 0; i < ids.size(); i++) {
                if (out[i] != null || !e.getValue().fallback().equals(ids.get(i).fallback())) continue;
                if (saved.containsKey(ids.get(i).primary())) continue;
                out[i] = e.getKey();
                used.add(e.getKey());
                break;
            }
        }
        return out;
    }
}
