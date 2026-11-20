package dev.henny.vibeyclient.module.hud;

import dev.henny.vibeyclient.api.Lang;

final class Directions {
    private Directions() {}

    private static final String[] SHORT = {"s", "sw", "w", "nw", "n", "ne", "e", "se"};
    private static final String[] AXIS = {"+Z", "-X +Z", "-X", "-X -Z", "-Z", "+X -Z", "+X", "+X +Z"};

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

    static float heading(float yaw) {
        return (((yaw + 180f) % 360f) + 360f) % 360f;
    }
}
