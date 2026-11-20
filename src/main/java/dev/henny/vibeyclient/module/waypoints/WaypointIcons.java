package dev.henny.vibeyclient.module.waypoints;

import dev.henny.vibeyclient.ui.Icons;

public final class WaypointIcons {
    public static final String PIN = "waypoint_pin";
    public static final String MAP = "waypoint_map";
    public static final String PORTAL = "waypoint_portal";

    private WaypointIcons() {}

    static void register() {
        Icons.register(PIN,
                "..#####..",
                ".#######.",
                "##.....##",
                "##.....##",
                ".##...##.",
                "..##.##..",
                "...###...",
                "....#....",
                ".........");
        Icons.register(MAP,
                "##.###.##",
                "#.##.##.#",
                "#..#..#.#",
                "#..#..#.#",
                "#..#..#.#",
                "#..#..#.#",
                "#.##.##.#",
                "##.###.##",
                ".........");
        Icons.register(PORTAL,
                ".#######.",
                ".#.....#.",
                ".#.#.#.#.",
                ".#..#..#.",
                ".#.#.#.#.",
                ".#..#..#.",
                ".#.#.#.#.",
                ".#.....#.",
                ".#######.");
    }
}
