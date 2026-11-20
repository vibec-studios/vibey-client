package dev.henny.companio.text;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Lang {
    public static final Lang EMPTY = new Lang(Map.of());

    private final Map<String, Map<String, List<String>>> tables;

    public Lang(Map<String, Map<String, List<String>>> tables) {
        Map<String, Map<String, List<String>>> m = new HashMap<>();
        tables.forEach((code, t) -> m.put(code.toLowerCase(Locale.ROOT), Map.copyOf(t)));
        this.tables = Map.copyOf(m);
    }

    public static String base(String language) {
        String l = language == null ? "en" : language.toLowerCase(Locale.ROOT);
        int i = l.indexOf('_');
        return i < 0 ? l : l.substring(0, i);
    }

    public boolean has(String key) {
        for (Map<String, List<String>> t : tables.values()) if (t.containsKey(key)) return true;
        return false;
    }

    public List<String> lookup(String key, String language) {
        String l = language == null ? "en_us" : language.toLowerCase(Locale.ROOT);
        List<String> v = get(l, key);
        if (v != null) return v;
        String base = base(l);
        for (Map.Entry<String, Map<String, List<String>>> e : tables.entrySet()) {
            if (base(e.getKey()).equals(base) && e.getValue().containsKey(key)) return e.getValue().get(key);
        }
        v = get("en_us", key);
        if (v != null) return v;
        for (Map.Entry<String, Map<String, List<String>>> e : tables.entrySet()) {
            if (base(e.getKey()).equals("en") && e.getValue().containsKey(key)) return e.getValue().get(key);
        }
        return null;
    }

    private List<String> get(String code, String key) {
        Map<String, List<String>> t = tables.get(code);
        return t == null ? null : t.get(key);
    }

    public int size() {
        return tables.size();
    }
}
