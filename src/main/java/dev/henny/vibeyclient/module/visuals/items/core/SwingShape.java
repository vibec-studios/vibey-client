package dev.henny.vibeyclient.module.visuals.items.core;

public record SwingShape(float tx, float ty, float tz, float yawIn, float roll, float pitch, float yawOut) {
    private static final float PI = (float) Math.PI;

    public static SwingShape of(SwingStyle style, float progress, int side) {
        float f = Math.max(0, Math.min(1, progress));
        float root = (float) Math.sqrt(f);
        float arc = sin(root * PI);
        float wave = sin(root * 2 * PI);
        float dip = sin(f * PI);
        float early = sin(f * f * PI);
        return switch (style) {
            case SMOOTH -> new SwingShape(side * -0.25f * arc, 0.1f * wave, -0.12f * dip,
                    side * (45f - 12f * early), side * -12f * arc, -50f * arc, side * -45f);
            case PUNCH -> new SwingShape(side * -0.12f * arc, 0.05f * arc, -0.35f * dip,
                    side * 45f, 0, -10f * arc, side * -45f);
            default -> new SwingShape(side * -0.4f * arc, 0.2f * wave, -0.2f * dip,
                    side * (45f - 20f * early), side * -20f * arc, -80f * arc, side * -45f);
        };
    }

    private static float sin(float v) {
        return (float) Math.sin(v);
    }
}
