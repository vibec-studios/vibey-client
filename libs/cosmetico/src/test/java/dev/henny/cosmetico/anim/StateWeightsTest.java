package dev.henny.cosmetico.anim;

import org.junit.jupiter.api.Test;

import static dev.henny.cosmetico.anim.StateWeights.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StateWeightsTest {
    private static final boolean[] OLD = has("idle", "walk", "fly", "sneak");
    private static final boolean[] ALL = has(NAMES);

    private static boolean[] has(String... names) {
        boolean[] h = new boolean[NAMES.length];
        for (String n : names) for (int i = 0; i < NAMES.length; i++) if (NAMES[i].equals(n)) h[i] = true;
        return h;
    }

    private static float[] w(boolean[] has, float glide, float fly, float move, float sprint, float sneak, float happy, float idleLong) {
        float[] out = new float[NAMES.length];
        StateWeights.compute(has, glide, fly, move, sprint, sneak, happy, idleLong, out);
        float sum = 0;
        for (float v : out) sum += v;
        assertEquals(1f, sum, 1e-5f, "weights sum to 1");
        return out;
    }

    @Test
    void standingStillPlaysIdle() {
        assertEquals(1f, w(ALL, 0, 0, 0, 0, 0, 0, 0)[IDLE], 1e-6f);
    }

    @Test
    void movingPetOfAFlyingPlayerNoLongerSumsAboveOne() {
        float[] o = w(OLD, 0, 1, 1, 0, 0, 0, 0);
        assertEquals(1f, o[FLY], 1e-6f);
        assertEquals(0f, o[WALK], 1e-6f);
    }

    @Test
    void oldBundlesFallBack() {
        assertEquals(1f, w(OLD, 1, 1, 0, 0, 0, 0, 0)[FLY], 1e-6f);
        assertEquals(1f, w(OLD, 0, 0, 1, 1, 0, 0, 0)[WALK], 1e-6f);
        assertEquals(1f, w(OLD, 0, 0, 0, 0, 0, 1, 1)[IDLE], 1e-6f);
    }

    @Test
    void newStatesTakePriority() {
        float[] elytra = w(ALL, 1, 1, 0, 0, 0, 0, 0);
        assertEquals(1f, elytra[GLIDE], 1e-6f);
        assertEquals(0f, elytra[FLY], 1e-6f);
        float[] sprint = w(ALL, 0, 0, 1, 1, 1, 0, 0);
        assertEquals(1f, sprint[SPRINT], 1e-6f);
        assertEquals(0f, sprint[SNEAK], 1e-6f);
        assertEquals(1f, w(ALL, 0, 0, 0, 0, 0, 1, 1)[HAPPY], 1e-6f);
        assertEquals(1f, w(ALL, 0, 0, 0, 0, 0, 0, 1)[IDLE_LONG], 1e-6f);
    }

    @Test
    void partialWeightsBlend() {
        float[] o = w(ALL, 0, 0, 0.4f, 0, 1, 0, 0);
        assertEquals(0.4f, o[WALK], 1e-6f);
        assertEquals(0.6f, o[SNEAK], 1e-6f);
        float[] half = w(ALL, 0, 0, 0, 0, 0, 0, 0.5f);
        assertTrue(half[IDLE_LONG] > 0.49f && half[IDLE] > 0.49f);
    }
}
