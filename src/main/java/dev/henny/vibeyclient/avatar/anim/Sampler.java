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

    /** Writes the channel value at {@code t} (seconds) into {@code out[off..off+2]}. */
    public static void sample(Channel c, float t, float[] out, int off) {
        float[] ts = c.t, v = c.v;
        int n = ts.length;
        if (n == 1 || t <= ts[0]) {
            // before the first key: its "pre" value if it has one? Blockbench holds the first key's value.
            out[off] = v[0];
            out[off + 1] = v[1];
            out[off + 2] = v[2];
            return;
        }
        if (t >= ts[n - 1]) {
            int k = (n - 1) * 3;
            out[off] = v[k];
            out[off + 1] = v[k + 1];
            out[off + 2] = v[k + 2];
            return;
        }
        int b = 1;
        while (b < n - 1 && ts[b] <= t) b++;
        int a = b - 1;
        float span = ts[b] - ts[a];
        float alpha = span <= 1e-6f ? 1f : (t - ts[a]) / span;
        Interp ia = c.interp[a], ib = c.interp[b];
        if (ia == Interp.STEP) {
            int k = a * 3;
            out[off] = v[k];
            out[off + 1] = v[k + 1];
            out[off + 2] = v[k + 2];
            return;
        }
        boolean catmull = ia == Interp.CATMULLROM || ib == Interp.CATMULLROM;
        float[] pre = c.pre;
        for (int axis = 0; axis < 3; axis++) {
            float p1 = v[a * 3 + axis];
            float p2 = Float.isNaN(pre[b * 3 + axis]) ? v[b * 3 + axis] : pre[b * 3 + axis];
            if (catmull) {
                float p0 = a > 0 ? v[(a - 1) * 3 + axis] : p1;
                float p3 = b < n - 1 ? v[(b + 1) * 3 + axis] : p2;
                out[off + axis] = catmullRom(alpha, p0, p1, p2, p3);
            } else {
                out[off + axis] = p1 + (p2 - p1) * alpha;
            }
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
