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
        java.util.List<String> all = new java.util.ArrayList<>();
        for (Prop p : Prop.values()) all.add(p.cssName());
        all.addAll(Shorthands.names());
        all.addAll(ALIASES.keySet());
        return dev.henny.vui.diag.Suggest.closest(name, all, 3);
    }
}
