package dev.henny.r3.text;

import dev.henny.r3.render.TextMetrics;

import java.util.Arrays;

public final class GlyphLayout {
    private int[] codepoints = new int[32];
    private float[] xs = new float[32];
    private float[] ys = new float[32];
    private float[] advances = new float[32];
    private int[] lines = new int[32];
    private int count;
    private int lineCount;
    private float width;
    private float height;
    private float lineHeight;

    public void layout(String text, TextMetrics metrics, float lineSpacing) {
        count = 0;
        lineHeight = metrics.lineHeight() * lineSpacing;
        String[] rows = text.split("\n", -1);
        lineCount = rows.length;
        width = 0;
        for (String row : rows) width = Math.max(width, metrics.width(row));
        height = lineCount * lineHeight;
        float top = -height / 2f;
        for (int line = 0; line < rows.length; line++) addRow(rows[line], line, top + line * lineHeight, metrics);
    }

    private void addRow(String row, int line, float y, TextMetrics metrics) {
        float x = -metrics.width(row) / 2f;
        for (int i = 0; i < row.length(); ) {
            int cp = row.codePointAt(i);
            float advance = metrics.advance(cp);
            ensure(count + 1);
            codepoints[count] = cp;
            xs[count] = x;
            ys[count] = y;
            advances[count] = advance;
            lines[count] = line;
            count++;
            x += advance;
            i += Character.charCount(cp);
        }
    }

    private void ensure(int size) {
        if (size <= codepoints.length) return;
        int n = Math.max(size, codepoints.length * 2);
        codepoints = Arrays.copyOf(codepoints, n);
        xs = Arrays.copyOf(xs, n);
        ys = Arrays.copyOf(ys, n);
        advances = Arrays.copyOf(advances, n);
        lines = Arrays.copyOf(lines, n);
    }

    public int count() {
        return count;
    }

    public int lineCount() {
        return lineCount;
    }

    public int codepoint(int i) {
        return codepoints[i];
    }

    public float x(int i) {
        return xs[i];
    }

    public float y(int i) {
        return ys[i];
    }

    public float advance(int i) {
        return advances[i];
    }

    public int line(int i) {
        return lines[i];
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public float lineHeight() {
        return lineHeight;
    }
}
