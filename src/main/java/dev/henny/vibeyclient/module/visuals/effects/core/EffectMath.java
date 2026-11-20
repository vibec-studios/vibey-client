package dev.henny.vibeyclient.module.visuals.effects.core;

public final class EffectMath {
    public static final float FIRE_ALPHA = 0.9f;

    private EffectMath() {}

    public static float scale(int percent) {
        return Math.max(0, percent) / 100f;
    }

    public static float fair(int percent, boolean blocked) {
        float s = scale(percent);
        return blocked ? Math.max(1f, s) : s;
    }

    public static float fireAlpha(int percent, boolean blocked) {
        return FIRE_ALPHA * Math.min(1f, fair(percent, blocked));
    }

    public static float overlayAlpha(float vanilla, int percent, boolean blocked) {
        return vanilla * Math.min(1f, fair(percent, blocked));
    }
}
