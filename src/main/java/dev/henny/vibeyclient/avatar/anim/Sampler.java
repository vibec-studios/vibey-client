package dev.henny.vibeyclient.avatar.anim;

import dev.henny.vibeyclient.avatar.bundle.Bundle.Channel;
import dev.henny.vibeyclient.avatar.bundle.Bundle.Interp;

/**
 * Keyframe sampling with Blockbench's rules (SPEC §5 "Format details"): segment A→B holds A if A is {@code step},
 * Catmull-Rom (neighbours as control points, clamped at the ends) if A or B is {@code catmullrom}, otherwise linear.
 * B's optional {@code pre} value is where the segment arrives. Before the first / after the last key the value is
 * held. No allocation.
 */
public final class Sampler {
    private Sampler() {}

    /**
     * Writes the channel value at {@code t} (seconds) into {@code out[off..off+2]}. Ported from the starter
     * content's reference renderer (Blockbench 5.2's BoneAnimator.interpolate / Keyframe.getCatmullromLerp): before
     * the first key its {@code pre ?? v} is held, after the last key its {@code v}; Catmull-Rom duplicates the end
     * point when there is no neighbour (or A/B is a split key) and, in loops with 3+ keys, wraps the missing
     * neighbours (keys[n-2] before the first segment, keys[1] after the last).
     */
    public static void sample(Channel c, float t, boolean loop, float[] out, int off) {
        float[] ts = c.t, v = c.v, pre = c.pre;
        int n = ts.length;
        if (t < ts[0]) {
            for (int k = 0; k < 3; k++) out[off + k] = Float.isNaN(pre[k]) ? v[k] : pre[k];
            return;
        }
        if (t >= ts[n - 1]) {
            int k = (n - 1) * 3;
            out[off] = v[k];
            out[off + 1] = v[k + 1];
            out[off + 2] = v[k + 2];
            return;
        }
        int a = 0;
        while (a < n - 1 && ts[a + 1] <= t) a++;
        int b = a + 1;
        float span = ts[b] - ts[a];
        float f = (t - ts[a]) / (span == 0 ? 1f : span);
        Interp ia = c.interp[a], ib = c.interp[b];
        if (ia == Interp.STEP) {
            out[off] = v[a * 3];
            out[off + 1] = v[a * 3 + 1];
            out[off + 2] = v[a * 3 + 2];
            return;
        }
        boolean aSplit = !Float.isNaN(pre[a * 3]), bSplit = !Float.isNaN(pre[b * 3]);
        if (ia == Interp.CATMULLROM || ib == Interp.CATMULLROM) {
            boolean wrap = loop && n >= 3;
            int before = a - 1 >= 0 ? a - 1 : (wrap ? n - 2 : -1);
            int after = b + 1 < n ? b + 1 : (wrap ? 1 : -1);
            for (int k = 0; k < 3; k++) {
                float p1 = v[a * 3 + k];
                float end = bSplit ? pre[b * 3 + k] : v[b * 3 + k];
                float p0 = before >= 0 && !aSplit ? v[before * 3 + k] : p1;
                float p3;
                if (after >= 0 && !bSplit) p3 = Float.isNaN(pre[after * 3 + k]) ? v[after * 3 + k] : pre[after * 3 + k];
                else p3 = end;
                out[off + k] = catmullRom(f, p0, p1, end, p3);
            }
            return;
        }
        for (int k = 0; k < 3; k++) {
            float p1 = v[a * 3 + k];
            float end = bSplit ? pre[b * 3 + k] : v[b * 3 + k];
            out[off + k] = p1 + (end - p1) * f;
        }
    }

    /** Uniform Catmull-Rom as in three.js (which Blockbench uses). */
    static float catmullRom(float t, float p0, float p1, float p2, float p3) {
        float v0 = (p2 - p0) * 0.5f;
        float v1 = (p3 - p1) * 0.5f;
        float t2 = t * t;
        float t3 = t * t2;
        return (2f * p1 - 2f * p2 + v0 + v1) * t3 + (-3f * p1 + 3f * p2 - 2f * v0 - v1) * t2 + v0 * t + p1;
    }

    /** Animation-local time for an animation that has run {@code elapsed} seconds. */
    public static float localTime(float elapsed, float length, boolean loop) {
        if (length <= 0) return 0;
        if (loop) {
            float t = elapsed % length;
            return t < 0 ? t + length : t;
        }
        return Math.min(Math.max(elapsed, 0), length);
    }
}
