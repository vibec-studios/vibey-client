package dev.henny.vibeyclient.module.screens.core;

public record Placement(Pos pos, float scale, int width, boolean hidden, String fallback) {
    public static final float MIN_SCALE = 0.5f;
    public static final float MAX_SCALE = 3f;
    public static final Placement DEFAULT = new Placement(null, 1f, 0, false, null);

    public Placement {
        scale = Float.isFinite(scale) ? Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale)) : 1f;
        width = Math.max(0, width);
    }

    public boolean isDefault() {
        return pos == null && scale == 1f && width == 0 && !hidden;
    }

    public Placement withPos(Pos p) {
        return new Placement(p, scale, width, hidden, fallback);
    }

    public Placement withScale(float s) {
        return new Placement(pos, s, width, hidden, fallback);
    }

    public Placement withWidth(int w) {
        return new Placement(pos, scale, w, hidden, fallback);
    }

    public Placement withHidden(boolean h) {
        return new Placement(pos, scale, width, h, fallback);
    }

    public Placement withFallback(String f) {
        return new Placement(pos, scale, width, hidden, f);
    }
}
