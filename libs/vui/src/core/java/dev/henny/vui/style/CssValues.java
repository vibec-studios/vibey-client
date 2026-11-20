package dev.henny.vui.style;

import java.util.ArrayList;
import java.util.List;

public final class CssValues {
    private CssValues() {
    }

    public static List<String> arguments(String call) {
        int open = call.indexOf('(');
        int close = call.lastIndexOf(')');
        if (open < 0 || close < open) throw new StyleError("missing brackets in '" + call + "'", null);
        return splitTopLevel(call.substring(open + 1, close), ',');
    }

    public static List<String> splitTopLevel(String s, char separator) {
        List<String> out = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == separator && depth == 0) {
                out.add(s.substring(start, i).trim());
                start = i + 1;
            }
        }
        String last = s.substring(start).trim();
        if (!last.isEmpty() || !out.isEmpty()) out.add(last);
        return out;
    }

    public static List<String> words(String s) {
        List<String> out = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            if (c == ')') depth--;
            if (Character.isWhitespace(c) && depth == 0) {
                if (current.length() > 0) out.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) out.add(current.toString());
        return out;
    }

    public static float time(String s) {
        String t = s.trim();
        try {
            if (t.endsWith("ms")) return Float.parseFloat(t.substring(0, t.length() - 2)) / 1000f;
            if (t.endsWith("s")) return Float.parseFloat(t.substring(0, t.length() - 1));
            return Float.parseFloat(t) / 1000f;
        } catch (NumberFormatException e) {
            throw new StyleError("'" + s + "' is not a duration", "use 150ms or 0.5s");
        }
    }

    public static boolean isTime(String s) {
        String t = s.trim();
        if (!(t.endsWith("ms") || t.endsWith("s"))) return false;
        String n = t.endsWith("ms") ? t.substring(0, t.length() - 2) : t.substring(0, t.length() - 1);
        try {
            Float.parseFloat(n);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
