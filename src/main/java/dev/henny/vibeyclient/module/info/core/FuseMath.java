package dev.henny.vibeyclient.module.info.core;

import java.util.Locale;

public final class FuseMath {
    public static final int DEFAULT_FUSE = 80;
    private static final int GREEN = 0xFF3FE0C5, YELLOW = 0xFFFFE14D, RED = 0xFFFF5470;

    private FuseMath() {}

    public static float seconds(int fuseTicks, float partialTick) {
        return Math.max(0f, (fuseTicks - partialTick) / 20f);
    }

    public static String label(float seconds) {
        return String.format(Locale.ROOT, "%.1f", Math.max(0f, seconds));
    }

    public static int color(float seconds) {
        float t = Math.max(0f, Math.min(1f, seconds / (DEFAULT_FUSE / 20f)));
        return t > 0.5f ? Colors.lerp(YELLOW, GREEN, (t - 0.5f) * 2f) : Colors.lerp(RED, YELLOW, t * 2f);
    }

    public static boolean urgent(float seconds) {
        return seconds < 1f;
    }
}
