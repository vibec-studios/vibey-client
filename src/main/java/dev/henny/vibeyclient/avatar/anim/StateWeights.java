package dev.henny.vibeyclient.avatar.anim;

import java.util.Arrays;

/**
 * Blend weights of the cosmetic animation states (SPEC §5). Pure function, no game classes (unit tested).
 * <p>
 * Priority with a capped budget of 1: {@code glide > fly > sprint > walk > sneak > happy > idle_long > idle}. Each
 * state takes its share of what the higher ones left, so the weights never sum above 1 (a moving pet of a flying
 * player no longer plays walk and fly both at 100 %). States whose animation is missing weigh 0 and leave their share
 * to the lower ones, with two fallbacks that keep 0.2.1 bundles unchanged: without {@code glide} elytra flight plays
 * {@code fly} ({@code fly} is 1 during elytra flight as well), without {@code sprint} sprinting plays {@code walk}.
 */
public final class StateWeights {
    private StateWeights() {}

    /** Animation names in weight order. */
    public static final String[] NAMES = {"idle", "walk", "fly", "sneak", "sprint", "glide", "idle_long", "happy"};
    public static final int IDLE = 0, WALK = 1, FLY = 2, SNEAK = 3, SPRINT = 4, GLIDE = 5, IDLE_LONG = 6, HAPPY = 7;

    /**
     * @param has    which animations the bundle has ({@link #NAMES} order)
     * @param glide  elytra flight or a long fall, 0..1
     * @param fly    creative flight or elytra flight, 0..1
     * @param move   walking speed 0..1 (pets: their own movement)
     * @param sprint sprinting, 0..1
     * @param out    receives the weights ({@link #NAMES} order)
     */
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
