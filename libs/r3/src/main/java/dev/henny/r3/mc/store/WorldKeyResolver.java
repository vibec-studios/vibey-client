package dev.henny.r3.mc.store;

import dev.henny.r3.mc.compat.Game;
import dev.henny.r3.persist.WorldKeys;

public final class WorldKeyResolver {
    public static final String UNKNOWN = "unknown";

    private WorldKeyResolver() {}

    public static String current() {
        String level = Game.singleplayerFolder();
        if (level != null) return WorldKeys.singleplayer(level);
        String address = Game.serverAddress();
        return address != null ? WorldKeys.server(address) : UNKNOWN;
    }
}
