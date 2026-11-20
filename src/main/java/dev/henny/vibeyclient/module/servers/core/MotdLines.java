package dev.henny.vibeyclient.module.servers.core;

import java.util.ArrayList;
import java.util.List;

public final class MotdLines {
    public static final int MAX_LINES = 4, MAX_CHARS = 120;

    public record Line(List<MotdSegment> segs) {}

    private MotdLines() {}

    public static List<Line> split(List<MotdSegment> segments) {
        List<Line> lines = new ArrayList<>();
        List<MotdSegment> current = new ArrayList<>();
        int chars = 0;
        for (MotdSegment s : segments) {
            String[] parts = s.text().replace("\r", "").split("\n", -1);
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    if (!close(lines, current)) return lines;
                    current = new ArrayList<>();
                    chars = 0;
                }
                String p = parts[i];
                if (p.isEmpty() || chars >= MAX_CHARS) continue;
                if (chars + p.length() > MAX_CHARS) p = p.substring(0, MAX_CHARS - chars);
                chars += p.length();
                current.add(new MotdSegment(p, s.color(), s.bold(), s.italic()));
            }
        }
        close(lines, current);
        while (!lines.isEmpty() && lines.get(lines.size() - 1).segs().isEmpty()) lines.remove(lines.size() - 1);
        return lines;
    }

    public static String plain(List<MotdSegment> segments) {
        StringBuilder b = new StringBuilder();
        for (MotdSegment s : segments) b.append(s.text());
        return b.toString();
    }

    private static boolean close(List<Line> lines, List<MotdSegment> current) {
        if (lines.size() >= MAX_LINES) return false;
        lines.add(new Line(List.copyOf(current)));
        return lines.size() < MAX_LINES;
    }
}
