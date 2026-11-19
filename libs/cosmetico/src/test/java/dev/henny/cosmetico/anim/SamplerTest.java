package dev.henny.cosmetico.anim;

import dev.henny.cosmetico.model.Channel;
import dev.henny.cosmetico.model.Interp;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SamplerTest {
    private static final float N = Float.NaN;

    private static Channel ch(float[] t, float[] v, float[] pre, Interp... in) {
        return new Channel(t, v, pre, in);
    }

    private static float x(Channel c, float t, boolean loop) {
        float[] out = new float[3];
        Sampler.sample(c, t, loop, out, 0);
        return out[0];
    }

    @Test
    void linearBetweenKeysAndHoldOutside() {
        Channel c = ch(new float[]{0, 1}, new float[]{0, 0, 0, 10, 0, 0}, new float[]{N, N, N, N, N, N}, Interp.LINEAR, Interp.LINEAR);
        assertEquals(5f, x(c, 0.5f, false), 1e-6f);
        assertEquals(0f, x(c, -1f, false), 1e-6f);
        assertEquals(10f, x(c, 3f, false), 1e-6f);
    }

    @Test
    void stepHoldsTheFirstKey() {
        Channel c = ch(new float[]{0, 1}, new float[]{2, 0, 0, 10, 0, 0}, new float[]{N, N, N, N, N, N}, Interp.STEP, Interp.LINEAR);
        assertEquals(2f, x(c, 0.99f, false), 1e-6f);
    }

    @Test
    void preSplitsTheArrivingKey() {
        Channel c = ch(new float[]{0, 1}, new float[]{0, 0, 0, 10, 0, 0}, new float[]{N, N, N, 4, 0, 0}, Interp.LINEAR, Interp.LINEAR);
        assertEquals(2f, x(c, 0.5f, false), 1e-6f);
        assertEquals(10f, x(c, 1f, false), 1e-6f);
    }

    @Test
    void preIsHeldBeforeTheFirstKey() {
        Channel c = ch(new float[]{0.5f, 1}, new float[]{3, 0, 0, 10, 0, 0}, new float[]{7, 0, 0, N, N, N}, Interp.LINEAR, Interp.LINEAR);
        assertEquals(7f, x(c, 0f, false), 1e-6f);
    }

    @Test
    void bezierFallsBackToLinear() {
        Channel c = ch(new float[]{0, 1}, new float[]{0, 0, 0, 10, 0, 0}, new float[]{N, N, N, N, N, N}, Interp.BEZIER, Interp.BEZIER);
        assertEquals(2.5f, x(c, 0.25f, false), 1e-6f);
    }

    @Test
    void catmullRomPassesThroughKeys() {
        Channel c = ch(new float[]{0, 1, 2}, new float[]{0, 0, 0, 10, 0, 0, 0, 0, 0}, new float[]{N, N, N, N, N, N, N, N, N},
                Interp.CATMULLROM, Interp.CATMULLROM, Interp.CATMULLROM);
        assertEquals(10f, x(c, 1f, true), 1e-5f);
        assertEquals(CatmullRom.at(0.5f, 10f, 0f, 10f, 0f), x(c, 0.5f, true), 1e-5f);
        assertEquals(CatmullRom.at(0.5f, 0f, 0f, 10f, 0f), x(c, 0.5f, false), 1e-5f);
    }

    @Test
    void localTimeLoopsAndClamps() {
        assertEquals(0.5f, Sampler.localTime(2.5f, 1f, true), 1e-6f);
        assertEquals(0.75f, Sampler.localTime(-0.25f, 1f, true), 1e-6f);
        assertEquals(1f, Sampler.localTime(5f, 1f, false), 1e-6f);
        assertEquals(0f, Sampler.localTime(-1f, 1f, false), 1e-6f);
    }
}
