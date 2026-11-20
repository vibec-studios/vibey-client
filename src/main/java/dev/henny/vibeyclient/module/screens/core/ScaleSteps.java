package dev.henny.vibeyclient.module.screens.core;

public final class ScaleSteps {
    static final float[] STEPS = {0.5f, 0.6f, 0.75f, 0.9f, 1f, 1.1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f};

    private ScaleSteps() {}

    public static float next(float current, int dir, float min, float max) {
        float out = current;
        if (dir > 0) {
            for (float s : STEPS) if (s > current + 0.001f) {
                out = s;
                break;
            }
        } else if (dir < 0) {
            for (int i = STEPS.length - 1; i >= 0; i--) if (STEPS[i] < current - 0.001f) {
                out = STEPS[i];
                break;
            }
        }
        return Math.max(min, Math.min(max, out));
    }

    public static int percent(float scale) {
        return Math.round(scale * 100);
    }
}
