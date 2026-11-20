package dev.henny.r3.text;

import dev.henny.r3.render.TextMetrics;

import java.util.ArrayList;
import java.util.List;

public final class TextWrap {
    private final List<String> lines = new ArrayList<>();
    private String lastText;
    private float lastWidth = -1f;
    private TextMetrics lastMetrics;

    public List<String> wrap(String text, TextMetrics metrics, float maxWidth) {
        if (text.equals(lastText) && maxWidth == lastWidth && metrics == lastMetrics) return lines;
        lastText = text;
        lastWidth = maxWidth;
        lastMetrics = metrics;
        lines.clear();
        split(text, metrics, maxWidth, lines);
        return lines;
    }

    public static void split(String text, TextMetrics metrics, float maxWidth, List<String> out) {
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            String next = current.isEmpty() ? word : current + " " + word;
            if (metrics.width(next) > maxWidth && !current.isEmpty()) {
                out.add(current.toString());
                current.setLength(0);
                current.append(word);
            } else {
                current.setLength(0);
                current.append(next);
            }
        }
        if (!current.isEmpty()) out.add(current.toString());
    }
}
