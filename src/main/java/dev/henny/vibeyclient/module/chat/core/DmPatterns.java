package dev.henny.vibeyclient.module.chat.core;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DmPatterns {
    private static final String NAME = "([A-Za-z0-9_]{2,16})";
    private static final String ARROW = " ?(?:->|→|») ?";
    private static final String ME = "(?:me|you|mir|dir|ich)";
    private static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static final List<Pattern> PATTERNS = List.of(
            Pattern.compile("^\\[" + NAME + ARROW + ME + "\\] ?(.+)$", FLAGS),
            Pattern.compile("^" + NAME + " whispers(?: to you)?: (.+)$", FLAGS),
            Pattern.compile("^" + NAME + " flüstert(?: dir zu)?: (.+)$", FLAGS),
            Pattern.compile("^(?:From|Von) " + NAME + ": (.+)$", FLAGS),
            Pattern.compile("^" + NAME + ARROW + ME + ": (.+)$", FLAGS));

    private DmPatterns() {}

    public static DirectMessage match(String plain, String ownName) {
        String s = plain.strip();
        for (Pattern p : PATTERNS) {
            Matcher m = p.matcher(s);
            if (!m.matches()) continue;
            String sender = m.group(1);
            if (ownName != null && sender.equalsIgnoreCase(ownName)) return null;
            return new DirectMessage(sender, m.group(2).strip());
        }
        return null;
    }
}
