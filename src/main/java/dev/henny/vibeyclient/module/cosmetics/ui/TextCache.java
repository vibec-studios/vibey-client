package dev.henny.vibeyclient.module.cosmetics.ui;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.ui.Gfx;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-frame text without per-frame allocation: a translated string with one argument (rebuilt only when the key,
 * argument or language change), and word wrapping of a string to a width (rebuilt when text or width change).
 */
final class TextCache {
    private String key;
    private String lang;
    private int intArg = Integer.MIN_VALUE;
    private String strArg;
    private String value;

    private String wrapText;
    private int wrapWidth = -1;
    private final List<String> lines = new ArrayList<>();

    /** {@code Lang.tr(key, n)}, cached. */
    String tr(String key, int n) {
        String l = Lang.code();
        if (value == null || !key.equals(this.key) || n != intArg || strArg != null || !l.equals(lang)) {
            this.key = key;
            this.lang = l;
            this.intArg = n;
            this.strArg = null;
            this.value = Lang.tr(key, n);
        }
        return value;
    }

    /** {@code Lang.tr(key, s)}, cached. */
    String tr(String key, String s) {
        String l = Lang.code();
        if (value == null || !key.equals(this.key) || !s.equals(strArg) || !l.equals(lang)) {
            this.key = key;
            this.lang = l;
            this.intArg = Integer.MIN_VALUE;
            this.strArg = s;
            this.value = Lang.tr(key, s);
        }
        return value;
    }

    /** {@code text} broken into lines of at most {@code width} pixels (at word boundaries, long words clipped). */
    List<String> wrap(Gfx g, String text, int width) {
        if (text.equals(wrapText) && width == wrapWidth) return lines;
        wrapText = text;
        wrapWidth = width;
        lines.clear();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() == 0) {
                line.append(word);
            } else if (g.width(line + " " + word) <= width) {
                line.append(' ').append(word);
            } else {
                lines.add(g.clip(line.toString(), width));
                line.setLength(0);
                line.append(word);
            }
        }
        if (line.length() > 0) lines.add(g.clip(line.toString(), width));
        return lines;
    }
}
