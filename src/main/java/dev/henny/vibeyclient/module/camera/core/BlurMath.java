package dev.henny.vibeyclient.module.camera.core;

public final class BlurMath {
    public static final int MIN_STRENGTH = 1;
    public static final int MAX_STRENGTH = 10;
    public static final double SECONDS_PER_STEP = 0.012;
    public static final float MAX_WEIGHT = 0.97f;
    public static final double JUMP_BLOCKS = 4.0;

    private BlurMath() {}

    public static float historyWeight(int strength, double dtSeconds) {
        if (strength < MIN_STRENGTH || !(dtSeconds > 0)) return 0f;
        int s = Math.min(MAX_STRENGTH, strength);
        double tau = SECONDS_PER_STEP * s;
        double w = Math.exp(-Math.min(dtSeconds, 1.0) / tau);
        return (float) Math.min(MAX_WEIGHT, w);
    }

    public static float trailSeconds(int strength) {
        return (float) (SECONDS_PER_STEP * Math.max(0, Math.min(MAX_STRENGTH, strength)));
    }

    public static boolean jumped(double dx, double dy, double dz) {
        return dx * dx + dy * dy + dz * dz > JUMP_BLOCKS * JUMP_BLOCKS;
    }
}
