package dev.henny.vui.paint;

import dev.henny.vui.style.ColorParser;
import dev.henny.vui.style.StyleError;
import dev.henny.vui.style.Theme;

import java.util.HashMap;
import java.util.Map;

public final class ThemeColors {
    private static int cachedVersion = -1;
    private static final Map<String, Integer> COLORS = new HashMap<>();

    private ThemeColors() {
    }

    public static int get(Theme theme, String name, int fallback) {
        if (theme == null) return fallback;
        if (theme.version() != cachedVersion) {
            COLORS.clear();
            cachedVersion = theme.version();
        }
        Integer cached = COLORS.get(name);
        if (cached != null) return cached;
        int value = fallback;
        String raw = theme.get(name);
        if (raw != null) {
            try {
                value = ColorParser.parse(theme.substitute(raw));
            } catch (StyleError | NumberFormatException ignored) {
                value = fallback;
            }
        }
        COLORS.put(name, value);
        return value;
    }
}
