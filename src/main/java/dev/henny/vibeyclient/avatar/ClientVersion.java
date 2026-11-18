package dev.henny.vibeyclient.avatar;

import dev.henny.vibeyclient.BuildInfo;

/** Compares {@code minClientVersion} strings (x.y.z[-pre][+build]) with the running Vibey Client version. */
public final class ClientVersion {
    private ClientVersion() {}

    /** True if this client is at least {@code required}. Unreadable requirements count as satisfied (logged by callers). */
    public static boolean satisfies(String required) {
        return compare(BuildInfo.VERSION, required) >= 0;
    }

    /** semver-like order: numeric core, a pre-release sorts before the release; build metadata is ignored. */
    public static int compare(String a, String b) {
        if (a == null || b == null) return 0;
        String[] pa = split(a), pb = split(b);
        String[] ca = pa[0].split("\\."), cb = pb[0].split("\\.");
        for (int i = 0; i < Math.max(ca.length, cb.length); i++) {
            long x = i < ca.length ? num(ca[i]) : 0, y = i < cb.length ? num(cb[i]) : 0;
            if (x != y) return Long.compare(x, y);
        }
        if (pa[1] == null) return pb[1] == null ? 0 : 1;
        if (pb[1] == null) return -1;
        return pa[1].compareTo(pb[1]);
    }

    private static String[] split(String v) {
        v = v.trim();
        if (v.startsWith("v")) v = v.substring(1);
        int plus = v.indexOf('+');
        if (plus >= 0) v = v.substring(0, plus);
        int dash = v.indexOf('-');
        return dash < 0 ? new String[]{v, null} : new String[]{v.substring(0, dash), v.substring(dash + 1)};
    }

    private static long num(String s) {
        try {
            return Long.parseLong(s.replaceAll("[^0-9].*$", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
