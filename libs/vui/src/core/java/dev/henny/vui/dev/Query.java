package dev.henny.vui.dev;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class Query {
    private final Map<String, String> values = new HashMap<>();

    public Query(URI uri) {
        String raw = uri.getRawQuery();
        if (raw == null || raw.isEmpty()) return;
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            String k = eq < 0 ? pair : pair.substring(0, eq);
            String v = eq < 0 ? "" : pair.substring(eq + 1);
            values.put(decode(k), decode(v));
        }
    }

    public String get(String key) {
        return values.get(key);
    }

    public String get(String key, String fallback) {
        String v = values.get(key);
        return v == null ? fallback : v;
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    public int number(String key, int fallback) {
        try {
            return values.containsKey(key) ? Integer.parseInt(values.get(key)) : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }
}
