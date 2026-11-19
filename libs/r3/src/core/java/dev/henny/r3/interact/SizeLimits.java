package dev.henny.r3.interact;

public record SizeLimits(float minWidth, float maxWidth, float minHeight, float maxHeight) {
    public static final SizeLimits DEFAULT = new SizeLimits(0.25f, 16f, 0.25f, 16f);
    public static final SizeLimits HUD_PIXELS = new SizeLimits(8f, 4096f, 8f, 4096f);

    public SizeLimits {
        if (minWidth <= 0 || minHeight <= 0 || maxWidth < minWidth || maxHeight < minHeight) {
            throw new IllegalArgumentException("bad size limits");
        }
    }

    public float clampWidth(float w) {
        return Math.max(minWidth, Math.min(maxWidth, w));
    }

    public float clampHeight(float h) {
        return Math.max(minHeight, Math.min(maxHeight, h));
    }

    public float minFactor(float width, float height) {
        return Math.max(minWidth / width, minHeight / height);
    }

    public float maxFactor(float width, float height) {
        return Math.min(maxWidth / width, maxHeight / height);
    }

    public float clampFactor(float factor, float width, float height) {
        float lo = minFactor(width, height);
        float hi = maxFactor(width, height);
        if (hi < lo) return 1f;
        return Math.max(lo, Math.min(hi, factor));
    }
}
