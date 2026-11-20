package dev.henny.vibeyclient.module.chat.core;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public record FilterRule(String source, Pattern regex, String plain) {
    public static final int MAX_LENGTH = 120;

    public static FilterRule parse(String source) {
        if (source == null) return null;
        String s = source.strip();
        if (s.isEmpty() || s.length() > MAX_LENGTH) return null;
        if (s.length() > 2 && s.startsWith("/") && s.endsWith("/")) return regex(s);
        return new FilterRule(s, null, Words.lower(s));
    }

    private static FilterRule regex(String s) {
        try {
            return new FilterRule(s, Pattern.compile(s.substring(1, s.length() - 1), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE), null);
        } catch (PatternSyntaxException e) {
            return null;
        }
    }

    public boolean matches(String text, String lower) {
        if (regex != null) return regex.matcher(text).find();
        return lower.contains(plain);
    }
}
