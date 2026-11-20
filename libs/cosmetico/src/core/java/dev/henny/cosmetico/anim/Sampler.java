package dev.henny.cosmetico.anim;

import dev.henny.cosmetico.model.Channel;
import dev.henny.cosmetico.model.Interp;

public final class Sampler {
    private Sampler() {}

    public static void sample(Channel c, float t, boolean loop, float[] out, int off) {
        float[] ts = c.t, v = c.v, pre = c.pre;
        int n = ts.length;
        if (t < ts[0]) {
            for (int k = 0; k < 3; k++) out[off + k] = Float.isNaN(pre[k]) ? v[k] : pre[k];
            return;
        }
        if (t >= ts[n - 1]) {
            copy(v, (n - 1) * 3, out, off);
            return;
        }
        int a = 0;
        while (a < n - 1 && ts[a + 1] <= t) a++;
        int b = a + 1;
        float span = ts[b] - ts[a];
        float f = (t - ts[a]) / (span == 0 ? 1f : span);
        Interp ia = c.interp[a], ib = c.interp[b];
        if (ia == Interp.STEP) {
            copy(v, a * 3, out, off);
            return;
        }
        if (ia == Interp.CATMULLROM || ib == Interp.CATMULLROM) catmull(c, a, b, f, loop, out, off);
        else linear(v, pre, a, b, f, out, off);
    }

    private static void copy(float[] v, int k, float[] out, int off) {
        out[off] = v[k];
        out[off + 1] = v[k + 1];
        out[off + 2] = v[k + 2];
    }

    private static void linear(float[] v, float[] pre, int a, int b, float f, float[] out, int off) {
        boolean bSplit = !Float.isNaN(pre[b * 3]);
        for (int k = 0; k < 3; k++) {
            float p1 = v[a * 3 + k];
            float end = bSplit ? pre[b * 3 + k] : v[b * 3 + k];
            out[off + k] = p1 + (end - p1) * f;
        }
    }

    private static void catmull(Channel c, int a, int b, float f, boolean loop, float[] out, int off) {
        float[] v = c.v, pre = c.pre;
        int n = c.t.length;
        boolean aSplit = !Float.isNaN(pre[a * 3]), bSplit = !Float.isNaN(pre[b * 3]);
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
            out[off + k] = CatmullRom.at(f, p0, p1, end, p3);
        }
    }

    public static float localTime(float elapsed, float length, boolean loop) {
        if (length <= 0) return 0;
        if (loop) {
            float t = elapsed % length;
            return t < 0 ? t + length : t;
        }
        return Math.min(Math.max(elapsed, 0), length);
    }
}
