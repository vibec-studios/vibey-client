package dev.henny.vui.layout;

public final class TextWrapper {
    public static final String ELLIPSIS = "…";

    private TextWrapper() {
    }

    public static void wrap(CharSequence text, GlyphWidths widths, float maxWidth, boolean wrap, int maxLines,
                            boolean ellipsis, float ellipsisWidth, TextLines out) {
        out.clear();
        int n = text.length();
        int lineStart = 0;
        float lineWidth = 0;
        int lastBreak = -1;
        float widthAtBreak = 0;
        int i = 0;
        while (i < n) {
            char c = text.charAt(i);
            if (c == '\n') {
                out.add(lineStart, i, lineWidth);
                lineStart = i + 1;
                lineWidth = 0;
                lastBreak = -1;
                i++;
                if (limitReached(out, maxLines)) {
                    finish(text, widths, maxWidth, ellipsis, ellipsisWidth, out, true);
                    return;
                }
                continue;
            }
            int cp = Character.codePointAt(text, i);
            int len = Character.charCount(cp);
            float w = widths.width(i, cp);
            if (wrap && lineWidth + w > maxWidth && i > lineStart) {
                int breakAt = lastBreak > lineStart ? lastBreak : i;
                float bw = lastBreak > lineStart ? widthAtBreak : lineWidth;
                out.add(lineStart, breakAt, bw);
                if (limitReached(out, maxLines)) {
                    finish(text, widths, maxWidth, ellipsis, ellipsisWidth, out, true);
                    return;
                }
                int next = breakAt;
                while (next < n && text.charAt(next) == ' ') next++;
                lineStart = next;
                lineWidth = measure(text, widths, lineStart, i);
                lastBreak = -1;
                if (next > i) {
                    i = next;
                    continue;
                }
            }
            if (c == ' ') {
                lastBreak = i;
                widthAtBreak = lineWidth;
            }
            lineWidth += w;
            i += len;
        }
        out.add(lineStart, n, lineWidth);
        finish(text, widths, maxWidth, ellipsis, ellipsisWidth, out, false);
    }

    private static boolean limitReached(TextLines out, int maxLines) {
        return maxLines > 0 && out.count() >= maxLines;
    }

    private static void finish(CharSequence text, GlyphWidths widths, float maxWidth, boolean ellipsis, float ellipsisWidth,
                               TextLines out, boolean truncated) {
        if (!ellipsis || out.count() == 0) return;
        int line = out.count() - 1;
        float width = out.width(line);
        if (!truncated && width <= maxWidth) return;
        int start = out.start(line);
        int end = out.end(line);
        float budget = maxWidth - ellipsisWidth;
        float w = 0;
        int cut = start;
        while (cut < end) {
            int cp = Character.codePointAt(text, cut);
            float gw = widths.width(cut, cp);
            if (w + gw > budget) break;
            w += gw;
            cut += Character.charCount(cp);
        }
        while (cut > start && text.charAt(cut - 1) == ' ') {
            cut--;
            w -= widths.width(cut, ' ');
        }
        out.replaceLast(cut, w + ellipsisWidth, true);
    }

    private static float measure(CharSequence text, GlyphWidths widths, int from, int to) {
        float w = 0;
        int i = from;
        while (i < to) {
            int cp = Character.codePointAt(text, i);
            w += widths.width(i, cp);
            i += Character.charCount(cp);
        }
        return w;
    }
}
