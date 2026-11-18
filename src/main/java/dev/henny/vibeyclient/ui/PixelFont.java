package dev.henny.vibeyclient.ui;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Blocky 5-row display font for the "VIBEY" wordmark and short display headings (the launcher uses a pixel font
 * for the same job). Drawn with fills, one cell = {@code cell} GUI pixels, so it is crisp at every GUI scale.
 * Supports A–Z, 0–9 and a few signs; anything else is skipped as a space. Use it for brand words and numbers only –
 * running text stays in the Minecraft font.
 */
public final class PixelFont {
    private PixelFont() {}

    public static final int ROWS = 5;

    private static final Map<Character, String[]> GLYPHS = new HashMap<>();

    private static void def(char c, String... rows) {
        GLYPHS.put(c, rows);
    }

    static {
        def('A', ".###.", "#...#", "#####", "#...#", "#...#");
        def('B', "####.", "#...#", "####.", "#...#", "####.");
        def('C', ".####", "#....", "#....", "#....", ".####");
        def('D', "####.", "#...#", "#...#", "#...#", "####.");
        def('E', "#####", "#....", "####.", "#....", "#####");
        def('F', "#####", "#....", "####.", "#....", "#....");
        def('G', ".####", "#....", "#..##", "#...#", ".####");
        def('H', "#...#", "#...#", "#####", "#...#", "#...#");
        def('I', "###", ".#.", ".#.", ".#.", "###");
        def('J', "..###", "....#", "....#", "#...#", ".###.");
        def('K', "#...#", "#..#.", "###..", "#..#.", "#...#");
        def('L', "#....", "#....", "#....", "#....", "#####");
        def('M', "#...#", "##.##", "#.#.#", "#...#", "#...#");
        def('N', "#...#", "##..#", "#.#.#", "#..##", "#...#");
        def('O', ".###.", "#...#", "#...#", "#...#", ".###.");
        def('P', "####.", "#...#", "####.", "#....", "#....");
        def('Q', ".###.", "#...#", "#...#", "#..#.", ".##.#");
        def('R', "####.", "#...#", "####.", "#..#.", "#...#");
        def('S', ".####", "#....", ".###.", "....#", "####.");
        def('T', "#####", "..#..", "..#..", "..#..", "..#..");
        def('U', "#...#", "#...#", "#...#", "#...#", ".###.");
        def('V', "#...#", "#...#", "#...#", ".#.#.", "..#..");
        def('W', "#...#", "#...#", "#.#.#", "##.##", "#...#");
        def('X', "#...#", ".#.#.", "..#..", ".#.#.", "#...#");
        def('Y', "#...#", ".#.#.", "..#..", "..#..", "..#..");
        def('Z', "#####", "...#.", "..#..", ".#...", "#####");
        def('0', ".##.", "#..#", "#..#", "#..#", ".##.");
        def('1', ".#.", "##.", ".#.", ".#.", "###");
        def('2', "###.", "...#", ".##.", "#...", "####");
        def('3', "###.", "...#", ".##.", "...#", "###.");
        def('4', "#..#", "#..#", "####", "...#", "...#");
        def('5', "####", "#...", "###.", "...#", "###.");
        def('6', ".##.", "#...", "###.", "#..#", ".##.");
        def('7', "####", "...#", "..#.", ".#..", ".#..");
        def('8', ".##.", "#..#", ".##.", "#..#", ".##.");
        def('9', ".##.", "#..#", ".###", "...#", ".##.");
        def('.', ".", ".", ".", ".", "#");
        def(',', "..", "..", "..", ".#", "#.");
        def(':', ".", "#", ".", "#", ".");
        def('-', "...", "...", "###", "...", "...");
        def('+', "...", ".#.", "###", ".#.", "...");
        def('/', "...#", "..#.", ".#..", "#...", "....");
        def('!', "#", "#", "#", ".", "#");
        def('?', "###.", "...#", ".##.", "....", ".#..");
        def('\'', "#", "#", ".", ".", ".");
        def('&', ".#..", "#.#.", ".#.#", "#.#.", ".#.#");
        def(' ', "..", "..", "..", "..", "..");
    }

    /** Width in GUI pixels of {@code s} at {@code cell} pixels per cell (1 cell gap between letters). */
    public static int width(String s, int cell) {
        int w = 0;
        String u = s.toUpperCase(Locale.ROOT);
        for (int i = 0; i < u.length(); i++) {
            String[] g = GLYPHS.getOrDefault(u.charAt(i), GLYPHS.get(' '));
            w += (g[0].length() + 1) * cell;
        }
        return Math.max(0, w - cell);
    }

    public static int height(int cell) {
        return ROWS * cell;
    }

    /** Draws {@code s} with its top left at x, y. */
    public static void draw(Gfx g, String s, int x, int y, int cell, int color) {
        String u = s.toUpperCase(Locale.ROOT);
        for (int i = 0; i < u.length(); i++) {
            String[] glyph = GLYPHS.getOrDefault(u.charAt(i), GLYPHS.get(' '));
            for (int r = 0; r < glyph.length; r++) {
                String row = glyph[r];
                int c = 0;
                while (c < row.length()) {
                    if (row.charAt(c) != '#') {
                        c++;
                        continue;
                    }
                    int start = c;
                    while (c < row.length() && row.charAt(c) == '#') c++;
                    g.rect(x + start * cell, y + r * cell, (c - start) * cell, cell, color);
                }
            }
            x += (glyph[0].length() + 1) * cell;
        }
    }

    /**
     * The wordmark look: letters in {@code color} over a one-cell "extrusion" in {@code shadow} (down and right),
     * like a pixel block seen from the front.
     */
    public static void drawExtruded(Gfx g, String s, int x, int y, int cell, int color, int shadow) {
        int d = Math.max(1, cell / 2);
        draw(g, s, x + d, y + d, cell, shadow);
        draw(g, s, x, y, cell, color);
    }
}
