package dev.henny.vibeyclient.module.cinematic;

import dev.henny.vibeyclient.module.cinematic.core.plan.Mode;

public final class ModeIcons {
    private ModeIcons() {}

    public static String of(Mode m) {
        return switch (m) {
            case BUILDS -> "cube";
            case PLAYERS -> "friends";
            case SELF -> "person";
            case PANORAMA -> "globe";
            case WAYPOINTS -> "pin";
            case SKY -> "star";
            case DRONIE -> "wings";
            case MIX -> "layers";
        };
    }
}
