package dev.henny.r3.vui;

final class GlyphStrings {
    private static final int CACHED = 0x3000;
    private static final String[] CACHE = new String[CACHED];

    private GlyphStrings() {}

    static String of(int codepoint) {
        if (codepoint < 0 || codepoint >= CACHED) return new String(Character.toChars(codepoint));
        String s = CACHE[codepoint];
        if (s == null) {
            s = new String(Character.toChars(codepoint));
            CACHE[codepoint] = s;
        }
        return s;
    }

    static String slice(CharSequence text, int start, int end) {
        if (text instanceof String s) return start == 0 && end == s.length() ? s : s.substring(start, end);
        return text.subSequence(start, end).toString();
    }
}
