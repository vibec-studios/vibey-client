package dev.henny.vibeyclient.module.cinematic;

import dev.henny.vibeyclient.api.ServerRules;

public final class CinematicRules {
    public static final String AFK = "cinematic.afk";
    public static final int MAX_RADIUS = 48;

    private CinematicRules() {}

    public static void declare() {
        ServerRules.supports(AFK);
    }

    public static boolean blocked() {
        return ServerRules.isDisabled(AFK);
    }
}
