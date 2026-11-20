package dev.henny.vui.expr;

import java.util.HashMap;
import java.util.Map;

public final class MapTranslator implements Translator {
    private final Map<String, String> strings = new HashMap<>();

    public MapTranslator put(String key, String value) {
        strings.put(key, value);
        return this;
    }

    public MapTranslator putAll(Map<String, String> values) {
        strings.putAll(values);
        return this;
    }

    public int size() {
        return strings.size();
    }

    public boolean has(String key) {
        return strings.containsKey(key);
    }

    @Override
    public String translate(String key, Object[] args) {
        String s = strings.getOrDefault(key, key);
        if (args == null || args.length == 0 || s.indexOf('{') < 0) return s;
        for (int i = 0; i < args.length; i++) s = s.replace("{" + i + "}", Values.display(args[i]));
        return s;
    }
}
