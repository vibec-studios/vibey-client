package dev.henny.vibeyclient.module.servers.core;

import java.util.ArrayList;
import java.util.List;

public final class LegacyFormat {
    private static final String[] COLORS = {"#000000", "#0000aa", "#00aa00", "#00aaaa", "#aa0000", "#aa00aa", "#ffaa00",
            "#aaaaaa", "#555555", "#5555ff", "#55ff55", "#55ffff", "#ff5555", "#ff55ff", "#ffff55", "#ffffff"};

    private LegacyFormat() {}

    public static List<MotdSegment> split(MotdSegment base) {
        String t = base.text();
        if (t.indexOf('§') < 0) return List.of(base);
        List<MotdSegment> out = new ArrayList<>();
        String color = base.color();
        boolean bold = base.bold(), italic = base.italic();
        StringBuilder run = new StringBuilder();
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c != '§' || i + 1 >= t.length()) {
                run.append(c);
                continue;
            }
            char code = Character.toLowerCase(t.charAt(++i));
            flush(out, run, color, bold, italic);
            int hex = Character.digit(code, 16);
            if (hex >= 0) {
                color = COLORS[hex];
                bold = false;
                italic = false;
            } else if (code == 'l') bold = true;
            else if (code == 'o') italic = true;
            else if (code == 'r') {
                color = base.color();
                bold = base.bold();
                italic = base.italic();
            }
        }
        flush(out, run, color, bold, italic);
        return out;
    }

    private static void flush(List<MotdSegment> out, StringBuilder run, String color, boolean bold, boolean italic) {
        if (run.isEmpty()) return;
        out.add(new MotdSegment(run.toString(), color, bold, italic));
        run.setLength(0);
    }
}
