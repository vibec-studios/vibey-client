package dev.henny.vibeyclient.module.cinematic.core;

import java.util.List;

public final class Spline {
    private static final int STEPS = 24;

    private final V3[] p;
    private final double[] arc;
    private final int segments;

    public Spline(List<V3> points) {
        if (points.isEmpty()) throw new IllegalArgumentException("no points");
        int n = points.size();
        p = new V3[n + 2];
        for (int i = 0; i < n; i++) p[i + 1] = points.get(i);
        p[0] = n > 1 ? points.get(0).mul(2).sub(points.get(1)) : points.get(0);
        p[n + 1] = n > 1 ? points.get(n - 1).mul(2).sub(points.get(n - 2)) : points.get(n - 1);
        segments = Math.max(1, n - 1);
        arc = new double[segments * STEPS + 1];
        V3 prev = raw(0, 0);
        for (int i = 1; i < arc.length; i++) {
            V3 cur = raw((i - 1) / STEPS, (double) (((i - 1) % STEPS) + 1) / STEPS);
            arc[i] = arc[i - 1] + cur.dist(prev);
            prev = cur;
        }
    }

    public static Spline of(V3... points) {
        return new Spline(List.of(points));
    }

    public double length() {
        return arc[arc.length - 1];
    }

    public V3 at(double u) {
        double total = length();
        if (total < 1e-9) return p[1];
        double want = Ease.clamp01(u) * total;
        int lo = 0, hi = arc.length - 1;
        while (hi - lo > 1) {
            int mid = (lo + hi) >>> 1;
            if (arc[mid] < want) lo = mid;
            else hi = mid;
        }
        double span = arc[hi] - arc[lo];
        double f = span < 1e-12 ? 0 : (want - arc[lo]) / span;
        double global = (lo + f) / STEPS;
        int seg = Math.min(segments - 1, (int) Math.floor(global));
        return raw(seg, global - seg);
    }

    private V3 raw(int seg, double s) {
        if (p.length == 3) return p[1];
        V3 p0 = p[seg], p1 = p[seg + 1], p2 = p[seg + 2], p3 = p[seg + 3];
        double t0 = 0;
        double t1 = t0 + knot(p0, p1);
        double t2 = t1 + knot(p1, p2);
        double t3 = t2 + knot(p2, p3);
        double t = t1 + (t2 - t1) * s;
        V3 a1 = mix(p0, p1, t0, t1, t);
        V3 a2 = mix(p1, p2, t1, t2, t);
        V3 a3 = mix(p2, p3, t2, t3, t);
        V3 b1 = mix(a1, a2, t0, t2, t);
        V3 b2 = mix(a2, a3, t1, t3, t);
        return mix(b1, b2, t1, t2, t);
    }

    private static double knot(V3 a, V3 b) {
        return Math.max(1e-4, Math.sqrt(a.dist(b)));
    }

    private static V3 mix(V3 a, V3 b, double ta, double tb, double t) {
        double w = (t - ta) / (tb - ta);
        return a.lerp(b, w);
    }
}
