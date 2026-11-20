package dev.henny.vibeyclient.module.chat.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Ranges {
    public record Range(int start, int end) {}

    private Ranges() {}

    public static List<Range> merge(List<Range> in) {
        if (in.size() < 2) return in;
        List<Range> sorted = new ArrayList<>(in);
        sorted.sort(Comparator.comparingInt(Range::start));
        List<Range> out = new ArrayList<>();
        Range cur = sorted.get(0);
        for (int i = 1; i < sorted.size(); i++) {
            Range r = sorted.get(i);
            if (r.start() <= cur.end()) {
                cur = new Range(cur.start(), Math.max(cur.end(), r.end()));
            } else {
                out.add(cur);
                cur = r;
            }
        }
        out.add(cur);
        return out;
    }

    public static List<Range> find(String text, String needle, boolean wholeWord) {
        List<Range> out = new ArrayList<>();
        if (needle.isEmpty()) return out;
        String hay = Words.lower(text);
        String n = Words.lower(needle);
        int from = 0;
        while (true) {
            int i = hay.indexOf(n, from);
            if (i < 0) return out;
            int end = i + n.length();
            if (!wholeWord || Words.wholeWordAt(text, i, end)) out.add(new Range(i, end));
            from = i + 1;
        }
    }
}
