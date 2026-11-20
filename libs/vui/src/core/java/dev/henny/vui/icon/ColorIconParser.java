package dev.henny.vui.icon;

import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.json.JsonValue;
import dev.henny.vui.style.ColorParser;
import dev.henny.vui.style.StyleError;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ColorIconParser {
    private static final String SHAPE = "{ \"palette\": { \"a\": \"#2b1b5e\", \"b\": \"#6f4bff\" }, \"rows\": [\".ab.\", ...] }";

    private ColorIconParser() {
    }

    static PixelIcon parse(String name, JsonValue value, Diagnostics d) {
        Map<Character, Integer> palette = palette(name, value, d);
        if (palette == null) return null;
        JsonValue rowsValue = value.get("rows");
        List<String> rows = rowsValue == null ? null : IconSetParser.rows(rowsValue);
        if (rows == null || rows.isEmpty()) {
            d.error(value.pos(), "colour icon '" + name + "' needs \"rows\"", SHAPE);
            return null;
        }
        int width = rows.get(0).length();
        int[] argb = new int[width * rows.size()];
        for (int y = 0; y < rows.size(); y++) {
            String r = rows.get(y);
            if (r.length() != width) {
                d.error(rowsValue.pos(), "icon '" + name + "': every row needs " + width + " characters", "row " + (y + 1) + " is \"" + r + "\"");
                return null;
            }
            for (int x = 0; x < width; x++) {
                char c = r.charAt(x);
                if (c == '.') continue;
                Integer color = palette.get(c);
                if (color == null) {
                    d.error(rowsValue.pos(), "icon '" + name + "': row " + (y + 1) + " uses '" + c + "', which is not in its palette",
                            "palette keys: " + palette.keySet() + ", '.' = transparent");
                    return null;
                }
                argb[y * width + x] = color;
            }
        }
        return PixelIcon.colored(name, width, rows.size(), argb);
    }

    private static Map<Character, Integer> palette(String name, JsonValue value, Diagnostics d) {
        JsonValue p = value.get("palette");
        if (p == null || !p.isObject() || p.asObject().isEmpty()) {
            d.error(value.pos(), "colour icon '" + name + "' needs a \"palette\" object", SHAPE);
            return null;
        }
        Map<Character, Integer> out = new HashMap<>();
        for (Map.Entry<String, JsonValue> e : p.asObject().entrySet()) {
            String key = e.getKey();
            if (key.length() != 1 || key.equals(".") || Character.isWhitespace(key.charAt(0))) {
                d.error(e.getValue().pos(), "icon '" + name + "': palette key '" + key + "' must be one character other than '.'", null);
                return null;
            }
            Integer color = color(name, key, e.getValue(), d);
            if (color == null) return null;
            out.put(key.charAt(0), color);
        }
        return out;
    }

    private static Integer color(String name, String key, JsonValue v, Diagnostics d) {
        if (!v.isString()) {
            d.error(v.pos(), "icon '" + name + "': palette '" + key + "' needs a colour string", "e.g. \"#6f4bff\"");
            return null;
        }
        try {
            int c = ColorParser.parse(v.asString());
            return (c >>> 24) == 0 ? 0 : c;
        } catch (StyleError err) {
            d.error(v.pos(), "icon '" + name + "': palette '" + key + "': " + err.getMessage(), err.hint());
            return null;
        }
    }
}
