package dev.henny.vibeyclient.ui.hud.edit;

public final class ScaleSteps {
    private ScaleSteps() {}

    static final float[] STEPS = {0.5f, 0.6f, 0.75f, 0.9f, 1f, 1.1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f, 3.5f, 4f};

    public static float next(float scale, int dir) {
        if (dir > 0) {
            for (float s : STEPS) if (s > scale + 0.001f) return s;
            return STEPS[STEPS.length - 1];
        }
        for (int i = STEPS.length - 1; i >= 0; i--) if (STEPS[i] < scale - 0.001f) return STEPS[i];
        return STEPS[0];
    }

    public static int percent(float scale) {
        return Math.round(scale * 100f);
    }
}
