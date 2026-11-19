package dev.henny.cosmetico.anim;

import java.util.Arrays;

public final class StateWeights {
    private StateWeights() {}

    public static final String[] NAMES = {"idle", "walk", "fly", "sneak", "sprint", "glide", "idle_long", "happy"};
    public static final int IDLE = 0, WALK = 1, FLY = 2, SNEAK = 3, SPRINT = 4, GLIDE = 5, IDLE_LONG = 6, HAPPY = 7;
    public static final int COUNT = NAMES.length;

    public static void compute(boolean[] has, float glide, float fly, float move, float sprint, float sneak, float happy,
                               float idleLong, float[] out) {
        Arrays.fill(out, 0f);
        float budget = 1f;
        if (has[GLIDE]) {
            out[GLIDE] = clamp01(glide);
            budget -= out[GLIDE];
        }
        if (has[FLY]) {
            out[FLY] = Math.min(clamp01(fly), budget);
            budget -= out[FLY];
        }
        float m = clamp01(move);
        float s = has[SPRINT] ? clamp01(sprint) : 0f;
        if (has[SPRINT]) out[SPRINT] = m * s * budget;
        if (has[WALK]) out[WALK] = m * (1f - s) * budget;
        budget -= out[SPRINT] + out[WALK];
        if (has[SNEAK]) {
            out[SNEAK] = clamp01(sneak) * budget;
            budget -= out[SNEAK];
        }
        if (has[HAPPY]) {
            out[HAPPY] = clamp01(happy) * budget;
            budget -= out[HAPPY];
        }
        if (has[IDLE_LONG]) {
            out[IDLE_LONG] = clamp01(idleLong) * budget;
            budget -= out[IDLE_LONG];
        }
        if (has[IDLE]) out[IDLE] = Math.max(0f, budget);
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(1f, v);
    }
}
