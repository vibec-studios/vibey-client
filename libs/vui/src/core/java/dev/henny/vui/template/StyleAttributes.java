package dev.henny.vui.template;

import java.util.Set;

public final class StyleAttributes {
    private static final Set<String> NAMES = Set.of("gap", "padding", "margin", "width", "height", "grow", "shrink",
            "basis", "align", "justify", "wrap", "anchor", "columns", "min-width", "max-width", "min-height",
            "max-height", "size", "flex", "min-column", "font-scale", "text-align", "opacity");

    private StyleAttributes() {
    }

    public static boolean isStyle(String name) {
        return NAMES.contains(name);
    }

    public static Set<String> names() {
        return NAMES;
    }
}
