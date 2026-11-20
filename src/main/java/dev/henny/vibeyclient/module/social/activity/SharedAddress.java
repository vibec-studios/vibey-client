package dev.henny.vibeyclient.module.social.activity;

import dev.henny.vibeyclient.module.social.model.Model.Activity;
import dev.henny.vibeyclient.module.social.model.Model.Join;
import dev.henny.vibeyclient.module.social.presence.ServerKind;

import java.util.Locale;

public final class SharedAddress {
    static final String HIDDEN = "multiplayer";

    private SharedAddress() {}

    public static String of(Activity a) {
        if (a == null || ServerKind.lan(a.serverKind())) return null;
        if (a.addressShared() != null) return a.addressShared() ? explicit(a) : null;
        Join j = a.join();
        if (a.joinable() && j != null && j.address() != null && !j.address().isBlank()) return withPort(j.address(), j.port());
        return looksLikeHost(a.server()) ? a.server().trim() : null;
    }

    public static String title(Activity a) {
        if (a == null) return null;
        if (!blank(a.serverName())) return a.serverName().trim();
        if (ServerKind.lan(a.serverKind())) return null;
        String server = blank(a.server()) ? null : a.server().trim();
        if (server != null && !looksLikeHost(server) && !server.equalsIgnoreCase(HIDDEN)) return server;
        return of(a);
    }

    public static boolean onServer(Activity a) {
        return a != null && (!blank(a.server()) || !blank(a.serverName()) || !blank(a.serverAddress()));
    }

    private static String explicit(Activity a) {
        if (blank(a.serverAddress())) return null;
        String h = a.serverAddress().trim();
        if (h.startsWith("[") && h.endsWith("]")) h = h.substring(1, h.length() - 1);
        boolean v6 = h.indexOf(':') >= 0;
        boolean defaultPort = a.serverPort() <= 0 || a.serverPort() == 25565;
        if (v6) return defaultPort ? "[" + h + "]" : "[" + h + "]:" + a.serverPort();
        return withPort(h, a.serverPort());
    }

    static String withPort(String host, int port) {
        return port <= 0 || port == 25565 ? host : host + ":" + port;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    public static boolean looksLikeHost(String s) {
        if (s == null) return false;
        String t = s.trim().toLowerCase(Locale.ROOT);
        if (t.isEmpty() || t.equals(HIDDEN) || t.length() > 255) return false;
        return t.matches("[a-z0-9._\\-]+(:\\d{1,5})?") && (t.contains(".") || t.startsWith("localhost"));
    }
}
