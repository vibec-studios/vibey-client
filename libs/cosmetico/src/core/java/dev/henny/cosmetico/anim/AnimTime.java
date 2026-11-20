package dev.henny.cosmetico.anim;

import dev.henny.cosmetico.model.Animation;

public final class AnimTime {
    private AnimTime() {}

    public static float global(Animation a, double now) {
        return a.loop ? (float) (now % a.length) : Sampler.localTime((float) (now % (a.length * 2)), a.length, false);
    }

    public static float state(double[] stateStart, int k, Animation a, double now) {
        if (k >= StateWeights.SPRINT && k < stateStart.length && !Double.isNaN(stateStart[k])) {
            double el = now - stateStart[k];
            return a.loop ? (float) (el % a.length) : (float) Math.min(el, a.length);
        }
        return global(a, now);
    }
}
