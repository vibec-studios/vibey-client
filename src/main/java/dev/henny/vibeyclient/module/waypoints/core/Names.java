package dev.henny.vibeyclient.module.waypoints.core;

import java.util.Collection;

public final class Names {
    private Names() {}

    public static String clean(String name) {
        if (name == null) return "";
        StringBuilder out = new StringBuilder();
        name.codePoints().filter(c -> c >= 0x20 && c != 0x7F && c != 0xA7).forEach(out::appendCodePoint);
        String s = out.toString().strip();
        return s.length() > Waypoint.MAX_NAME ? s.substring(0, Waypoint.MAX_NAME).strip() : s;
    }

    public static String next(String base, Collection<Waypoint> existing) {
        for (int n = 1; ; n++) {
            String candidate = base + " " + n;
            if (existing.stream().noneMatch(w -> w.name().equalsIgnoreCase(candidate))) return candidate;
        }
    }
}
