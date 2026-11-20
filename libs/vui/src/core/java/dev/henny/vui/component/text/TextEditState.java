package dev.henny.vui.component.text;

public final class TextEditState {
    private final StringBuilder text = new StringBuilder();
    private int caret;
    private int anchor;
    private int maxLength = Integer.MAX_VALUE;
    public float scroll;
    public float blink;

    public String text() {
        return text.toString();
    }

    public CharSequence chars() {
        return text;
    }

    public int length() {
        return text.length();
    }

    public int caret() {
        return caret;
    }

    public int anchor() {
        return anchor;
    }

    public void maxLength(int max) {
        maxLength = max <= 0 ? Integer.MAX_VALUE : max;
    }

    public boolean setText(String value) {
        String v = value == null ? "" : value;
        if (v.contentEquals(text)) return false;
        text.setLength(0);
        text.append(v);
        caret = Math.min(caret, text.length());
        anchor = Math.min(anchor, text.length());
        return true;
    }

    public boolean hasSelection() {
        return caret != anchor;
    }

    public int selectionStart() {
        return Math.min(caret, anchor);
    }

    public int selectionEnd() {
        return Math.max(caret, anchor);
    }

    public String selected() {
        return text.substring(selectionStart(), selectionEnd());
    }

    public void moveTo(int index, boolean extend) {
        caret = Math.max(0, Math.min(text.length(), index));
        if (!extend) anchor = caret;
        blink = 0;
    }

    public void selectAll() {
        anchor = 0;
        caret = text.length();
    }

    public boolean insert(String s) {
        String clean = sanitize(s);
        int removed = selectionEnd() - selectionStart();
        int room = maxLength - (text.length() - removed);
        if (room <= 0 && clean.length() > 0) return deleteSelection();
        if (clean.length() > room) clean = clean.substring(0, room);
        deleteSelection();
        text.insert(caret, clean);
        moveTo(caret + clean.length(), false);
        return true;
    }

    public boolean deleteSelection() {
        if (!hasSelection()) return false;
        int start = selectionStart();
        text.delete(start, selectionEnd());
        moveTo(start, false);
        return true;
    }

    public boolean backspace(boolean word) {
        if (deleteSelection()) return true;
        if (caret == 0) return false;
        int from = word ? WordBounds.previous(text, caret) : caret - 1;
        text.delete(from, caret);
        moveTo(from, false);
        return true;
    }

    public boolean delete(boolean word) {
        if (deleteSelection()) return true;
        if (caret >= text.length()) return false;
        int to = word ? WordBounds.next(text, caret) : caret + 1;
        text.delete(caret, to);
        moveTo(caret, false);
        return true;
    }

    public void left(boolean word, boolean extend) {
        if (hasSelection() && !extend) {
            moveTo(selectionStart(), false);
            return;
        }
        moveTo(word ? WordBounds.previous(text, caret) : caret - 1, extend);
    }

    public void right(boolean word, boolean extend) {
        if (hasSelection() && !extend) {
            moveTo(selectionEnd(), false);
            return;
        }
        moveTo(word ? WordBounds.next(text, caret) : caret + 1, extend);
    }

    private static String sanitize(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n' || c == '\r' || c == '\t') sb.append(' ');
            else if (c >= 32 && c != 127) sb.append(c);
        }
        return sb.toString();
    }
}
