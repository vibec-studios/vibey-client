package dev.henny.vui.style;

import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.diag.SourcePos;
import dev.henny.vui.json.JsonValue;
import dev.henny.vui.json.PosJsonParser;

import java.util.Map;

public final class ThemeLoader {
    private ThemeLoader() {
    }

    public static Theme load(String fileName, String source, Theme base, Diagnostics diagnostics) {
        Theme theme = base == null ? new Theme() : base.copy();
        String trimmed = source.trim();
        if (trimmed.startsWith("{")) readJson(fileName, source, theme, diagnostics);
        else readTokens(fileName, source, theme, diagnostics);
        return theme;
    }

    private static void readJson(String fileName, String source, Theme theme, Diagnostics diagnostics) {
        JsonValue root = PosJsonParser.parse(source, fileName, diagnostics);
        if (root == null) return;
        flatten("", root, theme, diagnostics);
    }

    private static void flatten(String prefix, JsonValue value, Theme theme, Diagnostics diagnostics) {
        for (Map.Entry<String, JsonValue> e : value.asObject().entrySet()) {
            JsonValue v = e.getValue();
            if (v.isObject() && prefix.isEmpty() && isGroup(e.getKey())) flatten(e.getKey(), v, theme, diagnostics);
            else if (v.isObject() || v.isArray()) diagnostics.error(v.pos(), "token '" + e.getKey() + "' must be a string or number", null);
            else theme.put(e.getKey(), v.asString());
        }
    }

    private static boolean isGroup(String key) {
        return key.equals("colors") || key.equals("sizes") || key.equals("tokens") || key.equals("times");
    }

    private static void readTokens(String fileName, String source, Theme theme, Diagnostics diagnostics) {
        String[] lines = source.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("//") || line.startsWith("/*")) continue;
            int colon = line.indexOf(':');
            if (!line.startsWith("$") || colon < 0) {
                diagnostics.error(new SourcePos(fileName, i + 1, 1), "expected '$name: value;'", "e.g. $brand: #8b6cff;");
                continue;
            }
            String name = line.substring(1, colon).trim();
            String value = line.substring(colon + 1).trim();
            if (value.endsWith(";")) value = value.substring(0, value.length() - 1).trim();
            theme.put(name, value);
        }
    }
}
