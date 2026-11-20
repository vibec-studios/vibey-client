package dev.henny.vibeyclient.module.waypoints.core;

final class NameEscape {
    private NameEscape() {}

    static String escape(String name) {
        return name.replace("%", "%25").replace(",", "%2C").replace("|", "%7C");
    }

    static String unescape(String name) {
        return name.replace("%7C", "|").replace("%2C", ",").replace("%25", "%");
    }
}
