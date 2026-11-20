package dev.henny.vibeyclient.module.chat.core;

import java.util.List;

public final class SenderPrefix {
    private static final int MAX_PREFIX = 40;
    private static final String CLOSERS = ">])";
    private static final String SEPARATORS = ":»>|";

    private SenderPrefix() {}

    public static boolean sentBy(String text, String name) {
        if (name == null || name.isEmpty()) return false;
        if (text.startsWith("<")) return vanillaSender(text, name);
        List<Ranges.Range> hits = Ranges.find(text, name, true);
        if (hits.isEmpty()) return false;
        Ranges.Range first = hits.get(0);
        if (first.start() > MAX_PREFIX || hasSeparator(text, first.start())) return false;
        return closesSender(text, first.end());
    }

    private static boolean vanillaSender(String text, String name) {
        int close = text.indexOf('>');
        return close > 0 && close <= MAX_PREFIX && !Ranges.find(text.substring(1, close), name, true).isEmpty();
    }

    private static boolean hasSeparator(String text, int before) {
        for (int i = 0; i < before; i++) if (SEPARATORS.indexOf(text.charAt(i)) >= 0 && text.charAt(i) != '>') return true;
        return false;
    }

    private static boolean closesSender(String text, int at) {
        if (at < text.length() && text.charAt(at) == '>') return true;
        int i = at;
        while (i < text.length() && CLOSERS.indexOf(text.charAt(i)) >= 0) i++;
        while (i < text.length() && text.charAt(i) == ' ') i++;
        return i < text.length() && SEPARATORS.indexOf(text.charAt(i)) >= 0;
    }
}
