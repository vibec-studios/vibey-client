package dev.henny.vui.render;

public interface TextMetrics {
    float glyphWidth(int codepoint, boolean bold);

    float lineHeight();

    default float width(CharSequence text, int start, int end, boolean bold) {
        float w = 0;
        int i = start;
        while (i < end) {
            int cp = Character.codePointAt(text, i);
            w += glyphWidth(cp, bold);
            i += Character.charCount(cp);
        }
        return w;
    }

    default float width(CharSequence text) {
        return width(text, 0, text.length(), false);
    }
}
