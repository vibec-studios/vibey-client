package dev.henny.vui.style;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class Theme {
    private static final AtomicInteger VERSIONS = new AtomicInteger(1);

    private final Map<String, String> tokens = new LinkedHashMap<>();
    private int version = VERSIONS.getAndIncrement();

    public Theme() {
    }

    public Theme(Map<String, String> tokens) {
        this.tokens.putAll(tokens);
    }

    public Theme put(String name, String value) {
        tokens.put(stripDollar(name), value);
        version = VERSIONS.getAndIncrement();
        return this;
    }

    public Theme putAll(Map<String, String> values) {
        for (Map.Entry<String, String> e : values.entrySet()) tokens.put(stripDollar(e.getKey()), e.getValue());
        version = VERSIONS.getAndIncrement();
        return this;
    }

    public Theme copy() {
        return new Theme(tokens);
    }

    public String get(String name) {
        return tokens.get(stripDollar(name));
    }

    public Map<String, String> tokens() {
        return Collections.unmodifiableMap(tokens);
    }

    public int version() {
        return version;
    }

    public String substitute(String raw) {
        return substitute(raw, 0);
    }

    private String substitute(String raw, int depth) {
        if (raw.indexOf('$') < 0) return raw;
        if (depth > 16) throw new StyleError("theme tokens refer to each other in a loop", "check the token values");
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (c != '$') {
                out.append(c);
                i++;
                continue;
            }
            int end = i + 1;
            while (end < raw.length() && isTokenChar(raw.charAt(end))) end++;
            String name = raw.substring(i + 1, end);
            String value = tokens.get(name);
            if (value == null) throw new StyleError("unknown theme token $" + name, suggestion(name));
            out.append(substitute(value, depth + 1));
            i = end;
        }
        return out.toString();
    }

    private String suggestion(String name) {
        String best = dev.henny.vui.diag.Suggest.closest(name, tokens.keySet(), 2);
        if (best != null) return "did you mean $" + best + "?";
        return "known tokens: $" + String.join(", $", tokens.keySet());
    }

    private static boolean isTokenChar(char c) {
        return Character.isLetterOrDigit(c) || c == '-' || c == '_';
    }

    private static String stripDollar(String name) {
        return name.startsWith("$") ? name.substring(1) : name;
    }
}
