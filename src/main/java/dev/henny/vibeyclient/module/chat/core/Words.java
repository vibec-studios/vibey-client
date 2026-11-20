package dev.henny.vibeyclient.module.chat.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class Words {
    private Words() {}

    public static boolean wordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    public static boolean wholeWordAt(String text, int start, int end) {
        boolean left = start == 0 || !wordChar(text.charAt(start - 1));
        boolean right = end >= text.length() || !wordChar(text.charAt(end));
        return left && right;
    }

    public static List<String> split(String csv) {
        Set<String> out = new LinkedHashSet<>();
        if (csv == null) return List.of();
        for (String part : csv.split("[,;\\n]")) {
            String w = part.trim();
            if (w.length() >= 2) out.add(w);
        }
        return new ArrayList<>(out);
    }

    public static String lower(String s) {
        return s.toLowerCase(Locale.ROOT);
    }
}
