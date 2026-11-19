package dev.henny.vibeyclient.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Small 9 × 9 pixel icons in the Vibe look, drawn with fills (crisp at every GUI scale, one colour, no textures).
 * Use the names below for menu entries ({@link MenuEntries}), tabs and buttons, or draw your own texture instead.
 */
public final class Icons {
    private Icons() {}

    public static final int SIZE = 9;

    public static final String PERSON = "person", SERVER = "server", SHIRT = "shirt", FRIENDS = "friends",
            SEAL = "seal", GEAR = "gear", PUZZLE = "puzzle", POWER = "power", HOST = "host", GLOBE = "globe",
            ACCESS = "access", SEARCH = "search", CLOSE = "close", BACK = "back", NEXT = "next", CHECK = "check",
            LINK = "link", KEY = "key", INFO = "info", WARN = "warn", CHAT = "chat", STAR = "star", MODULES = "modules",
            WAVE = "wave", USER = "user", NOTE = "note", CUBE = "cube", CROSSHAIR = "crosshair";

    private static final Map<String, String[]> ICONS = new HashMap<>();

    static {
        ICONS.put(PERSON, new String[] {
                "...###...",
                "..#####..",
                "..#####..",
                "...###...",
                ".........",
                "..#####..",
                ".#######.",
                ".#######.",
                "........."});
        ICONS.put(USER, ICONS.get(PERSON));
        ICONS.put(SERVER, new String[] {
                "#########",
                "#.......#",
                "#.##..#.#",
                "#########",
                ".........",
                "#########",
                "#.......#",
                "#.##..#.#",
                "#########"});
        ICONS.put(SHIRT, new String[] {
                ".........",
                ".##...##.",
                "####.####",
                "#########",
                "#.#####.#",
                "..#####..",
                "..#####..",
                "..#####..",
                "........."});
        ICONS.put(FRIENDS, new String[] {
                ".##...##.",
                "####.####",
                "####.####",
                ".##...##.",
                ".........",
                "####.####",
                "####.####",
                "####.####",
                "........."});
        ICONS.put(SEAL, new String[] {
                ".#######.",
                "#########",
                "##.###.##",
                "##.###.##",
                "#########",
                "##.....##",
                "##..#..##",
                "##.....##",
                ".#######."});
        ICONS.put(GEAR, new String[] {
                "....#....",
                ".#.###.#.",
                "..##.##..",
                ".##...##.",
                "###...###",
                ".##...##.",
                "..##.##..",
                ".#.###.#.",
                "....#...."});
        ICONS.put(PUZZLE, new String[] {
                "..###....",
                "..###....",
                "#######..",
                "#######..",
                "#########",
                "#########",
                "#######..",
                "#######..",
                "........."});
        ICONS.put(POWER, new String[] {
                "....#....",
                ".##.#.##.",
                "#...#...#",
                "#...#...#",
                "#.......#",
                "#.......#",
                "#.......#",
                ".##...##.",
                "...###..."});
        ICONS.put(HOST, new String[] {
                ".#.....#.",
                "#..#.#..#",
                "#.#.#.#.#",
                "#..#.#..#",
                ".#..#..#.",
                "....#....",
                "....#....",
                "...###...",
                "........."});
        ICONS.put(GLOBE, new String[] {
                "...###...",
                ".##.#.##.",
                ".#..#..#.",
                "#########",
                "#...#...#",
                "#########",
                ".#..#..#.",
                ".##.#.##.",
                "...###..."});
        ICONS.put(ACCESS, new String[] {
                "....#....",
                ".........",
                "#########",
                "....#....",
                "....#....",
                "...#.#...",
                "..#...#..",
                "..#...#..",
                "........."});
        ICONS.put(SEARCH, new String[] {
                ".####....",
                "#....#...",
                "#....#...",
                "#....#...",
                "#....#...",
                ".####....",
                ".....##..",
                "......##.",
                ".......##"});
        ICONS.put(CLOSE, new String[] {
                ".........",
                ".#.....#.",
                "..#...#..",
                "...#.#...",
                "....#....",
                "...#.#...",
                "..#...#..",
                ".#.....#.",
                "........."});
        ICONS.put(BACK, new String[] {
                ".........",
                "...#.....",
                "..#......",
                ".#.......",
                "#########",
                ".#.......",
                "..#......",
                "...#.....",
                "........."});
        ICONS.put(NEXT, new String[] {
                ".........",
                ".....#...",
                "......#..",
                ".......#.",
                "#########",
                ".......#.",
                "......#..",
                ".....#...",
                "........."});
        ICONS.put(CHECK, new String[] {
                ".........",
                "........#",
                ".......#.",
                "......#..",
                "#....#...",
                ".#..#....",
                "..##.....",
                "..#......",
                "........."});
        ICONS.put(LINK, new String[] {
                "....#####",
                "......###",
                ".....#.##",
                "....#...#",
                "####.....",
                "#........",
                "#........",
                "#........",
                "#####...."});
        ICONS.put(KEY, new String[] {
                "#########",
                "#.#.#.#.#",
                "#########",
                "#.#.#.#.#",
                "#########",
                "#..###..#",
                "#########",
                ".........",
                "........."});
        ICONS.put(INFO, new String[] {
                "#########",
                "#.......#",
                "#...#...#",
                "#.......#",
                "#..##...#",
                "#...#...#",
                "#...#...#",
                "#..###..#",
                "#########"});
        ICONS.put(WARN, new String[] {
                "....#....",
                "...###...",
                "...#.#...",
                "..##.##..",
                "..##.##..",
                ".#######.",
                ".###.###.",
                "#########",
                "........."});
        ICONS.put(CHAT, new String[] {
                "#########",
                "#.......#",
                "#.#.#.#.#",
                "#.......#",
                "#########",
                ".##......",
                ".#.......",
                ".........",
                "........."});
        ICONS.put(STAR, new String[] {
                "....#....",
                "....#....",
                "...###...",
                "#########",
                ".#######.",
                "..#####..",
                "..##.##..",
                ".##...##.",
                "........."});
        ICONS.put(MODULES, new String[] {
                "####.####",
                "#..#.#..#",
                "#..#.#..#",
                "####.####",
                ".........",
                "####.####",
                "#..#.#..#",
                "#..#.#..#",
                "####.####"});
        ICONS.put(NOTE, new String[] {
                "...######",
                "...######",
                "...#....#",
                "...#....#",
                "...#....#",
                ".###..###",
                "####.####",
                "####.####",
                ".##...##."});
        ICONS.put(CROSSHAIR, new String[] {
                "....#....",
                "..#####..",
                ".#.....#.",
                ".#.....#.",
                "###.#.###",
                ".#.....#.",
                ".#.....#.",
                "..#####..",
                "....#...."});
        ICONS.put(CUBE, new String[] {
                "....#....",
                "..##.##..",
                "##.....##",
                "#.##.##.#",
                "#...#...#",
                "#...#...#",
                "#...#...#",
                ".##.#.##.",
                "...###..."});
        ICONS.put(WAVE, new String[] {
                "..#.#.#..",
                "..#.#.#.#",
                "..#.#.#.#",
                "#.#######",
                "########.",
                ".#######.",
                "..#####..",
                "...###...",
                "........."});
    }

    /** Adds (or replaces) an icon: 9 rows of 9 characters, '#' = filled. For module-owned icon sets. */
    public static synchronized void register(String name, String... rows) {
        if (rows.length != SIZE) throw new IllegalArgumentException("Icon " + name + " needs " + SIZE + " rows");
        ICONS.put(name, rows);
    }

    public static boolean has(String name) {
        return name != null && ICONS.containsKey(name);
    }

    public static Set<String> names() {
        return ICONS.keySet();
    }

    /** Draws icon {@code name} (9 × 9 cells) with its top left at x, y; one cell = {@code cell} GUI pixels. */
    public static void draw(Gfx g, String name, int x, int y, int cell, int color) {
        String[] rows = ICONS.get(name);
        if (rows == null) return;
        for (int r = 0; r < rows.length; r++) {
            String row = rows[r];
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
    }

    public static void draw(Gfx g, String name, int x, int y, int color) {
        draw(g, name, x, y, 1, color);
    }
}
