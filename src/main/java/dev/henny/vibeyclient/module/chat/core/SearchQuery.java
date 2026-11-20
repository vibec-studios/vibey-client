package dev.henny.vibeyclient.module.chat.core;

import java.util.ArrayList;
import java.util.List;

public final class SearchQuery {
    public static final SearchQuery NONE = new SearchQuery("", List.of());
    public static final int MAX_TERMS = 8;

    private final String text;
    private final List<String> terms;

    private SearchQuery(String text, List<String> terms) {
        this.text = text;
        this.terms = terms;
    }

    public static SearchQuery of(String text) {
        if (text == null || text.isBlank()) return NONE;
        List<String> t = new ArrayList<>();
        for (String part : Words.lower(text.strip()).split("\\s+")) {
            if (!part.isEmpty() && t.size() < MAX_TERMS) t.add(part);
        }
        return new SearchQuery(text.strip(), List.copyOf(t));
    }

    public String text() {
        return text;
    }

    public boolean active() {
        return !terms.isEmpty();
    }

    public boolean matches(String plain) {
        String lower = Words.lower(plain);
        for (String t : terms) if (!lower.contains(t)) return false;
        return true;
    }

    public List<Ranges.Range> ranges(String plain) {
        List<Ranges.Range> out = new ArrayList<>();
        for (String t : terms) out.addAll(Ranges.find(plain, t, false));
        return Ranges.merge(out);
    }
}
