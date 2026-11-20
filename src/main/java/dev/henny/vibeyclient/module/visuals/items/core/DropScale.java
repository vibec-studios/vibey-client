package dev.henny.vibeyclient.module.visuals.items.core;

public final class DropScale {
    private DropScale() {}

    public static float vanillaLift(float vanillaY, float modelMinY, float scale) {
        return vanillaY + modelMinY * (1 - scale);
    }

    public static float physicsLift(float lift, float scale) {
        return lift * scale;
    }

    public static float factor(int percent) {
        return Math.max(0.1f, percent / 100f);
    }
}
