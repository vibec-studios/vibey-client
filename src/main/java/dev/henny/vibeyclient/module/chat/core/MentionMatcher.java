package dev.henny.vibeyclient.module.chat.core;

import java.util.ArrayList;
import java.util.List;

public final class MentionMatcher {
    private final List<String> words;
    private final String ownName;

    public MentionMatcher(String ownName, List<String> extraWords) {
        this.ownName = ownName == null ? "" : ownName;
        List<String> w = new ArrayList<>();
        if (this.ownName.length() >= 2) w.add(this.ownName);
        for (String x : extraWords) if (!x.equalsIgnoreCase(this.ownName)) w.add(x);
        this.words = List.copyOf(w);
    }

    public List<String> words() {
        return words;
    }

    public List<Ranges.Range> ranges(String text) {
        List<Ranges.Range> out = new ArrayList<>();
        for (String w : words) out.addAll(Ranges.find(text, w, true));
        return Ranges.merge(out);
    }

    public boolean mentions(String text) {
        if (words.isEmpty() || SenderPrefix.sentBy(text, ownName)) return false;
        return !ranges(text).isEmpty();
    }
}
