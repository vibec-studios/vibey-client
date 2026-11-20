package dev.henny.vibeyclient.module.social.activity;

import dev.henny.vibeyclient.module.social.model.Model.Activity;
import dev.henny.vibeyclient.module.social.model.Model.Join;

import java.util.Locale;

public final class SharedAddress {
    static final String HIDDEN = "multiplayer";

    private SharedAddress() {}

    public static String of(Activity a) {
        if (a == null) return null;
        Join j = a.join();
        if (a.joinable() && j != null && j.address() != null && !j.address().isBlank()) return withPort(j.address(), j.port());
        return looksLikeHost(a.server()) ? a.server().trim() : null;
    }

    static String withPort(String host, int port) {
        return port <= 0 || port == 25565 ? host : host + ":" + port;
    }

    public static boolean looksLikeHost(String s) {
        if (s == null) return false;
        String t = s.trim().toLowerCase(Locale.ROOT);
        if (t.isEmpty() || t.equals(HIDDEN) || t.length() > 255) return false;
        return t.matches("[a-z0-9._\\-]+(:\\d{1,5})?") && (t.contains(".") || t.startsWith("localhost"));
    }
}
