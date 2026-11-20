package dev.henny.vibeyclient.api.privacy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class NameRedactor {
    public static final NameRedactor NONE = new NameRedactor(List.of(), null);

    public record Hit(int start, int end, String name) {}

    private final List<String> names;
    private final NameAliases aliases;

    private NameRedactor(List<String> names, NameAliases aliases) {
        this.names = names;
        this.aliases = aliases;
    }

    public static NameRedactor of(Collection<String> names, NameAliases aliases) {
        Set<String> unique = new LinkedHashSet<>();
        for (String n : names) if (n != null && n.length() >= 2) unique.add(n);
        List<String> sorted = new ArrayList<>(unique);
        sorted.sort(Comparator.comparingInt(String::length).reversed());
        return new NameRedactor(List.copyOf(sorted), aliases);
    }

    public boolean active() {
        return !names.isEmpty();
    }

    public List<String> names() {
        return names;
    }

    public String alias(String name) {
        return aliases == null ? name : aliases.alias(name);
    }

    public List<Hit> hits(String text) {
        List<Hit> out = new ArrayList<>();
        if (names.isEmpty() || text.isEmpty()) return out;
        String lower = text.toLowerCase(Locale.ROOT);
        boolean[] taken = new boolean[text.length()];
        for (String n : names) collect(text, lower, n, taken, out);
        out.sort(Comparator.comparingInt(Hit::start));
        return out;
    }

    private static void collect(String text, String lower, String name, boolean[] taken, List<Hit> out) {
        String n = name.toLowerCase(Locale.ROOT);
        int from = 0;
        while (true) {
            int i = lower.indexOf(n, from);
            if (i < 0) return;
            int end = i + n.length();
            from = i + 1;
            if (!boundary(text, i, end) || anyTaken(taken, i, end)) continue;
            for (int k = i; k < end; k++) taken[k] = true;
            out.add(new Hit(i, end, name));
        }
    }

    private static boolean boundary(String text, int start, int end) {
        boolean left = start == 0 || !wordChar(text.charAt(start - 1));
        boolean right = end >= text.length() || !wordChar(text.charAt(end));
        return left && right;
    }

    private static boolean wordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static boolean anyTaken(boolean[] taken, int start, int end) {
        for (int k = start; k < end; k++) if (taken[k]) return true;
        return false;
    }

    public String redact(String text) {
        List<Hit> hits = hits(text);
        if (hits.isEmpty()) return text;
        StringBuilder sb = new StringBuilder(text.length() + 16);
        int at = 0;
        for (Hit h : hits) {
            sb.append(text, at, h.start()).append(alias(h.name()));
            at = h.end();
        }
        return sb.append(text, at, text.length()).toString();
    }
}
