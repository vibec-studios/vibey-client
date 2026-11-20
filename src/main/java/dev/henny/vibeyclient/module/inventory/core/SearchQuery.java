package dev.henny.vibeyclient.module.inventory.core;

import java.util.ArrayList;
import java.util.List;

public final class SearchQuery {
    public static final SearchQuery NONE = new SearchQuery("", List.of());

    private record Term(String text, boolean mod, boolean negate) {}

    private final String raw;
    private final List<Term> terms;

    private SearchQuery(String raw, List<Term> terms) {
        this.raw = raw;
        this.terms = terms;
    }

    public static SearchQuery parse(String raw) {
        if (raw == null || raw.isBlank()) return NONE;
        List<Term> terms = new ArrayList<>();
        for (String word : raw.trim().split("\\s+")) {
            boolean negate = word.startsWith("-") && word.length() > 1;
            String w = negate ? word.substring(1) : word;
            boolean mod = w.startsWith("@");
            String text = TextFold.fold(mod ? w.substring(1) : w);
            if (!text.isEmpty()) terms.add(new Term(text, mod, negate));
        }
        return terms.isEmpty() ? NONE : new SearchQuery(raw, List.copyOf(terms));
    }

    public boolean isEmpty() {
        return terms.isEmpty();
    }

    public String raw() {
        return raw;
    }

    public boolean matches(ItemFacts item) {
        if (item == null) return false;
        for (Term t : terms) {
            boolean hit = t.mod ? item.mod().startsWith(t.text) : item.contains(t.text);
            if (hit == t.negate) return false;
        }
        return true;
    }
}
