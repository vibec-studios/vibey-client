package dev.henny.vibeyclient.module.info.core;

import java.util.Locale;

public final class HealthRule {
    private HealthRule() {}

    public static boolean visible(boolean ownVehicle, boolean hasBossBar) {
        return ownVehicle || hasBossBar;
    }

    public static String label(float health, float max) {
        return String.format(Locale.ROOT, "%d/%d", (int) Math.ceil(Math.max(0f, health)), Math.round(max));
    }
}
