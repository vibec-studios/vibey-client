package dev.henny.vibeyclient.module.chat.core;

import java.util.ArrayList;
import java.util.List;

public final class ChatFilters {
    public static final int MAX_RULES = 64;
    public static final int MAX_TEXT = 1024;
    public static final ChatFilters NONE = new ChatFilters(List.of());

    private final List<FilterRule> rules;

    private ChatFilters(List<FilterRule> rules) {
        this.rules = rules;
    }

    public static ChatFilters of(List<String> sources) {
        List<FilterRule> out = new ArrayList<>();
        for (String s : sources) {
            FilterRule r = FilterRule.parse(s);
            if (r != null && out.size() < MAX_RULES) out.add(r);
        }
        return new ChatFilters(List.copyOf(out));
    }

    public static boolean valid(String source) {
        return FilterRule.parse(source) != null;
    }

    public int size() {
        return rules.size();
    }

    public boolean hides(String plain) {
        if (rules.isEmpty()) return false;
        String text = plain.length() > MAX_TEXT ? plain.substring(0, MAX_TEXT) : plain;
        String lower = Words.lower(text);
        for (FilterRule r : rules) if (r.matches(text, lower)) return true;
        return false;
    }
}
