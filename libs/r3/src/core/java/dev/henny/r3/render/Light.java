package dev.henny.r3.render;

public final class Light {
    public static final int FULL_BRIGHT = pack(15, 15);
    public static final int DARK = pack(0, 0);

    private Light() {}

    public static int pack(int block, int sky) {
        return (clamp(block) << 4) | (clamp(sky) << 20);
    }

    public static int block(int packed) {
        return packed >> 4 & 0xF;
    }

    public static int sky(int packed) {
        return packed >> 20 & 0xF;
    }

    public static int brightest(int a, int b) {
        return pack(Math.max(block(a), block(b)), Math.max(sky(a), sky(b)));
    }

    private static int clamp(int level) {
        return Math.max(0, Math.min(15, level));
    }
}
