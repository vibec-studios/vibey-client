package dev.henny.vibeafk;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatParser {
    private static final Pattern CHAT = Pattern.compile("\\[CHAT\\] (.*)$");
    private static final Pattern USER = Pattern.compile("Setting user: (\\S+)");
    private static final Pattern COLOR = Pattern.compile("§.");
    private static final Pattern ANGLE = Pattern.compile("^<([^<>\\s]{1,32})> ");
    private static final Pattern BRACKET = Pattern.compile("^\\[([^\\[\\]\\s]{1,32})\\] ");
    private static final Pattern RANKED = Pattern.compile("^(?:\\[[^\\]]{1,24}\\] )?([A-Za-z0-9_]{3,16})\\s*(?:»|>>|:) ");
    private static final Pattern[] WHISPER = {
            Pattern.compile("^([A-Za-z0-9_]{1,16}) whispers to you: "),
            Pattern.compile("^([A-Za-z0-9_]{1,16}) flüstert dir zu: "),
            Pattern.compile("^\\[([A-Za-z0-9_]{1,16}) -> (?:me|mir|you|dir)\\] ", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^\\[(?:MSG|PM|DM)\\] \\[?([A-Za-z0-9_]{1,16})\\]? -> (?:me|mir|you|dir)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^From ([A-Za-z0-9_]{1,16}): "),
            Pattern.compile("^Von ([A-Za-z0-9_]{1,16}): "),
            Pattern.compile("^([A-Za-z0-9_]{1,16}) -> (?:you|dir|me|mir): ", Pattern.CASE_INSENSITIVE),
    };

    private ChatParser() {}

    public static final class Line {
        public final String text;
        public final String sender;
        public final boolean whisper;

        Line(String text, String sender, boolean whisper) {
            this.text = text;
            this.sender = sender;
            this.whisper = whisper;
        }

        public boolean mentions(String name) {
            if (name == null || name.isEmpty()) return false;
            if (sender != null && sender.equalsIgnoreCase(name)) return false;
            String body = text;
            if (sender != null) {
                int at = text.indexOf(sender);
                if (at >= 0 && at < 40) body = text.substring(at + sender.length());
            }
            Matcher m = Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(name) + "(?![A-Za-z0-9_])",
                    Pattern.CASE_INSENSITIVE).matcher(body);
            return m.find();
        }
    }

    public static String clean(String s) {
        String t = COLOR.matcher(s).replaceAll("");
        StringBuilder b = new StringBuilder(t.length());
        for (int k = 0; k < t.length(); k++) {
            char c = t.charAt(k);
            if (c >= 0x20 && c != 0x7f) b.append(c);
        }
        String r = b.toString().trim();
        return r.length() > 1024 ? r.substring(0, 1024) : r;
    }

    public static Line parseLogLine(String logLine) {
        Matcher m = CHAT.matcher(logLine);
        if (!m.find()) return null;
        String text = clean(m.group(1));
        if (text.isEmpty()) return null;
        return classify(text);
    }

    public static Line classify(String text) {
        for (Pattern p : WHISPER) {
            Matcher w = p.matcher(text);
            if (w.find()) return new Line(text, w.group(1), true);
        }
        Matcher a = ANGLE.matcher(text);
        if (a.find()) return new Line(text, a.group(1), false);
        Matcher r = RANKED.matcher(text);
        if (r.find()) return new Line(text, r.group(1), false);
        Matcher b = BRACKET.matcher(text);
        if (b.find() && !b.group(1).toLowerCase(Locale.ROOT).equals("server")) return new Line(text, b.group(1), false);
        return new Line(text, null, false);
    }

    public static String userFromLog(String logLine) {
        Matcher m = USER.matcher(logLine);
        return m.find() ? m.group(1) : null;
    }
}
