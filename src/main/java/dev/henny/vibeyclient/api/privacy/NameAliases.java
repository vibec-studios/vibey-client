package dev.henny.vibeyclient.api.privacy;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class NameAliases {
    public static final int MAX_NAMES = 512;

    private final Map<String, Integer> numbers = new HashMap<>();
    private String own = "";
    private String ownAlias = "Me";
    private String otherFormat = "Player %d";

    public synchronized void own(String name, String alias) {
        own = name == null ? "" : name.toLowerCase(Locale.ROOT);
        ownAlias = alias;
    }

    public synchronized void otherFormat(String format) {
        otherFormat = format;
    }

    public synchronized String alias(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        if (key.equals(own)) return ownAlias;
        Integer n = numbers.get(key);
        if (n == null) {
            if (numbers.size() >= MAX_NAMES) numbers.clear();
            n = numbers.size() + 1;
            numbers.put(key, n);
        }
        return String.format(Locale.ROOT, otherFormat, n);
    }

    public synchronized void clear() {
        numbers.clear();
    }
}
