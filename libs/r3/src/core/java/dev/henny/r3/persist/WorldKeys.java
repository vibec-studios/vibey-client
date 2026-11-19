package dev.henny.r3.persist;

import java.util.Locale;

public final class WorldKeys {
    private WorldKeys() {}

    public static String singleplayer(String levelName) {
        return "sp:" + levelName;
    }

    public static String server(String address) {
        return "mp:" + address.trim().toLowerCase(Locale.ROOT);
    }

    public static String realm(String realmId) {
        return "realm:" + realmId;
    }
}
