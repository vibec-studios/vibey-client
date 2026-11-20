package dev.henny.vui.mc.draw;

final class TextStrings {
    private static final int SLOTS = 1024;

    private final CharSequence[] sources = new CharSequence[SLOTS];
    private final int[] starts = new int[SLOTS];
    private final int[] ends = new int[SLOTS];
    private final String[] values = new String[SLOTS];
    private final String[][] glyphPages = new String[256][];

    String of(CharSequence text, int start, int end) {
        if (start == 0 && end == text.length() && text instanceof String s) return s;
        int slot = (System.identityHashCode(text) * 31 + start * 17 + end) & (SLOTS - 1);
        if (sources[slot] == text && starts[slot] == start && ends[slot] == end && same(values[slot], text, start)) return values[slot];
        String value = text.subSequence(start, end).toString();
        sources[slot] = text;
        starts[slot] = start;
        ends[slot] = end;
        values[slot] = value;
        return value;
    }

    private static boolean same(String cached, CharSequence text, int start) {
        for (int i = 0; i < cached.length(); i++) if (cached.charAt(i) != text.charAt(start + i)) return false;
        return true;
    }

    String glyph(int codepoint) {
        if (codepoint >= 0x10000) return new String(Character.toChars(codepoint));
        String[] page = glyphPages[codepoint >>> 8];
        if (page == null) page = glyphPages[codepoint >>> 8] = new String[256];
        String s = page[codepoint & 0xFF];
        if (s == null) s = page[codepoint & 0xFF] = String.valueOf((char) codepoint);
        return s;
    }
}
