package dev.henny.companio.text;

public final class Sanitize {
    public static final int BUBBLE_MAX = 40;
    public static final int NAME_MAX = 24;

    private Sanitize() {}

    public static String bubble(String s) {
        return clean(s, BUBBLE_MAX);
    }

    public static String name(String s) {
        return clean(s, NAME_MAX).trim();
    }

    public static String clean(String s, int max) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(Math.min(s.length(), max));
        int count = 0;
        for (int i = 0; i < s.length() && count < max; ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == '§') {
                if (i < s.length()) i += Character.charCount(s.codePointAt(i));
                continue;
            }
            if (Character.isISOControl(cp) && cp != '\n') continue;
            if (Character.getType(cp) == Character.FORMAT) continue;
            sb.appendCodePoint(cp);
            count++;
        }
        return sb.toString();
    }
}
