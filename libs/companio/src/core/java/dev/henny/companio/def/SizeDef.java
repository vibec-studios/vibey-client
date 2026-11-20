package dev.henny.companio.def;

public record SizeDef(float def, float min, float max) {
    public static final SizeDef ONE = new SizeDef(1f, 1f, 1f);

    public float clamp(float v) {
        return Math.max(min, Math.min(max, v));
    }

    public boolean adjustable() {
        return max > min;
    }
}
