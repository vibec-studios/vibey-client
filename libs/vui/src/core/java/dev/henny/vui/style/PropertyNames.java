package dev.henny.vui.style;

import java.util.Map;

import static java.util.Map.entry;

public final class PropertyNames {
    private static final Map<String, String> ALIASES = Map.ofEntries(
            entry("grow", "flex-grow"), entry("shrink", "flex-shrink"), entry("basis", "flex-basis"),
            entry("wrap", "flex-wrap"), entry("align", "align-items"), entry("justify", "justify-content"),
            entry("background", "background"), entry("bg", "background"), entry("columns", "grid-columns"),
            entry("min-column", "grid-min-column"), entry("font-size", "font-scale"), entry("accent", "accent-color"),
            entry("text-color", "color"), entry("x", "left"), entry("y", "top"), entry("flex-direction", "display"),
            entry("border-width", "border-width"), entry("white-space", "text-wrap"));

    private PropertyNames() {
    }

    public static String canonical(String name) {
        String n = name.trim().toLowerCase();
        return ALIASES.getOrDefault(n, n);
    }

    public static boolean isKnown(String name) {
        String c = canonical(name);
        return Prop.byName(c) != null || Shorthands.isShorthand(c);
    }

    public static String suggest(String name) {
        String best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Prop p : Prop.values()) {
            int d = distance(name, p.cssName());
            if (d < bestDistance) {
                bestDistance = d;
                best = p.cssName();
            }
        }
        for (String s : Shorthands.names()) {
            int d = distance(name, s);
            if (d < bestDistance) {
                bestDistance = d;
                best = s;
            }
        }
        return bestDistance <= 3 ? best : null;
    }

    private static int distance(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return prev[b.length()];
    }
}
