package dev.henny.vibeyclient.module.screens.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WidgetIds {
    public record Fact(String type, String key) {}

    public record Id(String primary, String fallback) {}

    private WidgetIds() {}

    public static List<Id> assign(List<Fact> facts) {
        Map<String, Integer> byKey = new HashMap<>();
        Map<String, Integer> byType = new HashMap<>();
        List<Id> out = new ArrayList<>(facts.size());
        for (Fact f : facts) {
            String type = clean(f.type());
            int ti = byType.merge(type, 1, Integer::sum) - 1;
            String fallback = type + "@" + ti;
            String key = f.key() == null || f.key().isBlank() ? null : f.key();
            if (key == null) {
                out.add(new Id(fallback, fallback));
                continue;
            }
            String base = type + ":" + key;
            int occ = byKey.merge(base, 1, Integer::sum) - 1;
            out.add(new Id(occ == 0 ? base : base + "#" + occ, fallback));
        }
        return out;
    }

    private static String clean(String type) {
        return type == null || type.isBlank() ? "widget" : type;
    }
}
