package dev.henny.vui.icon;

import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.json.JsonValue;
import dev.henny.vui.json.PosJsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class IconSetParser {
    private IconSetParser() {
    }

    public static IconSet parse(String namespace, String fileName, String source, Diagnostics diagnostics) {
        IconSet set = new IconSet(namespace);
        JsonValue root = PosJsonParser.parse(source, fileName, diagnostics);
        if (root == null || !root.isObject()) return set;
        JsonValue icons = root.get("icons");
        if (icons == null || !icons.isObject()) {
            diagnostics.error(root.pos(), "icons.json needs an \"icons\" object", "{ \"icons\": { \"plus\": [\"..#..\", ...] } }");
            return set;
        }
        for (Map.Entry<String, JsonValue> e : icons.asObject().entrySet()) icon(set, e.getKey(), e.getValue(), diagnostics);
        JsonValue aliases = root.get("aliases");
        if (aliases != null && aliases.isObject()) {
            for (Map.Entry<String, JsonValue> a : aliases.asObject().entrySet()) {
                String target = a.getValue().asString();
                if (set.get(target) == null) diagnostics.error(a.getValue().pos(), "alias '" + a.getKey() + "' points to unknown icon '" + target + "'", null);
                else set.alias(a.getKey(), target);
            }
        }
        return set;
    }

    private static void icon(IconSet set, String name, JsonValue value, Diagnostics diagnostics) {
        List<String> rows = rows(value);
        if (rows == null || rows.isEmpty()) {
            diagnostics.error(value.pos(), "icon '" + name + "' needs rows", "a list of strings or one string of rows split by spaces, '#' = on, '.' = off");
            return;
        }
        int width = rows.get(0).length();
        for (String r : rows) {
            if (r.length() != width || !r.matches("[#.]+")) {
                diagnostics.error(value.pos(), "icon '" + name + "': every row needs " + width + " characters of '#' and '.'", "bad row \"" + r + "\"");
                return;
            }
        }
        set.put(PixelIcon.fromRows(name, rows));
    }

    private static List<String> rows(JsonValue value) {
        List<String> out = new ArrayList<>();
        if (value.isArray()) {
            for (JsonValue v : value.asArray()) out.add(v.asString().trim());
            return out;
        }
        if (!value.isString()) return null;
        for (String part : value.asString().trim().split("\\s+")) if (!part.isEmpty()) out.add(part);
        return out;
    }
}
