package dev.henny.vibeyclient.ui.toast;

import dev.henny.vibeyclient.ui.Gfx;

import java.util.ArrayList;
import java.util.List;

/** Word wrapping with the Minecraft font (long words are cut). Call it once and cache the result, not per frame. */
public final class TextWrap {
    private TextWrap() {}

    /** Lines of at most {@code width} pixels; {@code maxLines > 0} clips the last line with "…". */
    public static List<String> wrap(Gfx g, String text, int width, int maxLines) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        width = Math.max(20, width);
        for (String para : text.replace("\r", "").split("\n", -1)) {
            StringBuilder line = new StringBuilder();
            for (String word : para.split(" ", -1)) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (g.width(candidate) <= width) {
                    line.setLength(0);
                    line.append(candidate);
                    continue;
                }
                if (line.length() > 0) {
                    out.add(line.toString());
                    line.setLength(0);
                }
                // word longer than a line: cut it
                String w = word;
                while (g.width(w) > width) {
                    int n = Math.max(1, g.font.plainSubstrByWidth(w, width).length());
                    out.add(w.substring(0, n));
                    w = w.substring(n);
                }
                line.append(w);
            }
            out.add(line.toString());
        }
        if (maxLines > 0 && out.size() > maxLines) {
            List<String> cut = new ArrayList<>(out.subList(0, maxLines));
            String last = cut.get(maxLines - 1);
            cut.set(maxLines - 1, g.clip(last + " " + out.get(maxLines), width));
            if (!cut.get(maxLines - 1).endsWith("…")) cut.set(maxLines - 1, g.clip(last + "……", width));
            return cut;
        }
        return out;
    }
}
