package dev.henny.r3.mc.render;

final class GlyphCache {
    private static final int CACHED = 0x3000;
    private static final String[] STRINGS = new String[CACHED];

    private GlyphCache() {}

    static String of(int codepoint) {
        if (codepoint < 0 || codepoint >= CACHED) return new String(Character.toChars(codepoint));
        String s = STRINGS[codepoint];
        if (s == null) STRINGS[codepoint] = s = new String(Character.toChars(codepoint));
        return s;
    }
}
