package dev.henny.vibeyclient.module.hud;

import dev.henny.vibeyclient.api.Lang;

/** Compass directions from a yaw (Minecraft: 0 = south, 90 = west, 180 = north, 270 = east). */
final class Directions {
    private Directions() {}

    private static final String[] SHORT = {"s", "sw", "w", "nw", "n", "ne", "e", "se"};
    private static final String[] AXIS = {"+Z", "-X +Z", "-X", "-X -Z", "-Z", "+X -Z", "+X", "+X +Z"};

    /** 0..7 starting at south, clockwise on the compass. */
    static int octant(float yaw) {
        float y = ((yaw % 360f) + 360f) % 360f;
        return Math.round(y / 45f) % 8;
    }

    static String shortName(float yaw) {
        return Lang.tr("hud.dir." + SHORT[octant(yaw)]);
    }

    static String full(float yaw) {
        int o = octant(yaw);
        return Lang.tr("hud.dir." + SHORT[o] + ".full") + " (" + AXIS[o] + ")";
    }

    /** Compass heading in degrees, 0 = north, 90 = east. */
    static float heading(float yaw) {
        return (((yaw + 180f) % 360f) + 360f) % 360f;
    }
}
