package dev.henny.companio.text;

import java.util.List;
import java.util.Map;
import java.util.Random;

public record TextRef(String literal, Map<String, String> perLanguage, String key, List<TextRef> options) {
    public static final TextRef EMPTY = literal("");

    public static TextRef literal(String s) {
        return new TextRef(s, null, null, null);
    }

    public static TextRef languages(Map<String, String> map) {
        return new TextRef(null, Map.copyOf(map), null, null);
    }

    public static TextRef key(String key) {
        return new TextRef(null, null, key, null);
    }

    public static TextRef anyOf(List<TextRef> options) {
        return new TextRef(null, null, null, List.copyOf(options));
    }

    public String resolve(Lang lang, String language) {
        return resolve(lang, language, null);
    }

    public String resolve(Lang lang, String language, Random random) {
        if (literal != null) return literal;
        if (options != null) return options.isEmpty() ? "" : pick(options, random).resolve(lang, language, random);
        if (perLanguage != null) return perLanguage(language);
        List<String> values = lang == null ? null : lang.lookup(key, language);
        if (values == null || values.isEmpty()) return "@" + key;
        return values.size() == 1 || random == null ? values.get(0) : values.get(random.nextInt(values.size()));
    }

    private String perLanguage(String language) {
        String base = Lang.base(language);
        String v = perLanguage.get(base);
        if (v == null) v = perLanguage.get("en");
        if (v == null && !perLanguage.isEmpty()) v = perLanguage.values().iterator().next();
        return v == null ? "" : v;
    }

    private static TextRef pick(List<TextRef> list, Random random) {
        return random == null ? list.get(0) : list.get(random.nextInt(list.size()));
    }
}
