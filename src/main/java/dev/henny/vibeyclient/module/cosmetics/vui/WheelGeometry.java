package dev.henny.vibeyclient.module.cosmetics.vui;

public record WheelGeometry(float cx, float cy, float tile, float radius, float centreSize) {
    public static final int SLOTS = 8;
    private static final float MIN_TILE = 18;
    private static final float MAX_TILE = 44;
    private static final float SPREAD = 1.31f;
    private static final float GAP = 4;

    public static WheelGeometry fit(float cx, float cy, float diameter) {
        float tile = clamp(diameter / 4.4f, MIN_TILE, MAX_TILE);
        float radius = (diameter - tile) / 2 - 2;
        while (radius < SPREAD * (tile + GAP) && tile > MIN_TILE) {
            tile--;
            radius = (diameter - tile) / 2 - 2;
        }
        radius = Math.min(radius, tile * 2.2f);
        float centre = Math.max(tile, 2 * radius - tile - 8);
        return new WheelGeometry(cx, cy, Math.round(tile), Math.round(radius), Math.round(centre));
    }

    public float sliceX(int i) {
        return Math.round(cx + radius * (float) Math.sin(angle(i)) - tile / 2);
    }

    public float sliceY(int i) {
        return Math.round(cy - radius * (float) Math.cos(angle(i)) - tile / 2);
    }

    public float centreX() {
        return Math.round(cx - centreSize / 2);
    }

    public float centreY() {
        return Math.round(cy - centreSize / 2);
    }

    public float dotX(int i) {
        return Math.round(cx + radius * 0.5f * (float) Math.sin(angle(i)) - 2);
    }

    public float dotY(int i) {
        return Math.round(cy - radius * 0.5f * (float) Math.cos(angle(i)) - 2);
    }

    public float top() {
        return cy - radius - tile / 2;
    }

    public float bottom() {
        return cy + radius + tile / 2;
    }

    public int direction(double x, double y) {
        double dx = x - cx, dy = y - cy;
        double dead = radius * 0.3;
        if (dx * dx + dy * dy < dead * dead) return -1;
        int i = (int) Math.round(Math.atan2(dx, -dy) / (Math.PI / 4));
        return Math.floorMod(i, SLOTS);
    }

    public int hit(double x, double y) {
        for (int i = 0; i < SLOTS; i++) {
            float sx = sliceX(i), sy = sliceY(i);
            if (x >= sx && y >= sy && x < sx + tile && y < sy + tile) return i;
        }
        return -1;
    }

    private static double angle(int i) {
        return i * Math.PI / 4;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
