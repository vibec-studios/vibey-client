package dev.henny.vibeyclient.module.info.card;

import dev.henny.vibeyclient.ui.Icons;

public final class InfoIcons {
    public static final String HEART = "info_heart", SPROUT = "info_sprout", LIGHT = "info_light";
    private static boolean done;

    private InfoIcons() {}

    public static synchronized void init() {
        if (done) return;
        done = true;
        Icons.register(HEART,
                ".........",
                ".##...##.",
                "####.####",
                "#########",
                "#########",
                ".#######.",
                "..#####..",
                "...###...",
                "....#....");
        Icons.register(SPROUT,
                ".........",
                "##.....##",
                "###...###",
                ".###.###.",
                "...###...",
                "....#....",
                "....#....",
                "....#....",
                "..#####..");
        Icons.register(LIGHT,
                "....#....",
                ".#.....#.",
                "...###...",
                "..#####..",
                "#.#####.#",
                "..#####..",
                "...###...",
                ".#.....#.",
                "....#....");
    }
}
