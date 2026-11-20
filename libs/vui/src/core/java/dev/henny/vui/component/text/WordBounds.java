package dev.henny.vui.component.text;

public final class WordBounds {
    private WordBounds() {
    }

    public static int previous(CharSequence s, int from) {
        int i = Math.max(0, from - 1);
        while (i > 0 && Character.isWhitespace(s.charAt(i))) i--;
        while (i > 0 && !Character.isWhitespace(s.charAt(i - 1))) i--;
        return i;
    }

    public static int next(CharSequence s, int from) {
        int n = s.length();
        int i = Math.min(n, from);
        while (i < n && !Character.isWhitespace(s.charAt(i))) i++;
        while (i < n && Character.isWhitespace(s.charAt(i))) i++;
        return i;
    }
}
