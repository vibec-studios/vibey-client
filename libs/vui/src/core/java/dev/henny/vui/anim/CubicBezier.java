package dev.henny.vui.anim;

public record CubicBezier(float x1, float y1, float x2, float y2) implements Easing {

    @Override
    public float apply(float t) {
        if (t <= 0f) return 0f;
        if (t >= 1f) return 1f;
        float s = solveCurveX(t);
        return curve(s, y1, y2);
    }

    private float solveCurveX(float x) {
        float s = x;
        for (int i = 0; i < 8; i++) {
            float error = curve(s, x1, x2) - x;
            if (Math.abs(error) < 1e-5f) return s;
            float slope = slope(s, x1, x2);
            if (Math.abs(slope) < 1e-6f) break;
            s -= error / slope;
        }
        float lo = 0f;
        float hi = 1f;
        s = x;
        for (int i = 0; i < 30; i++) {
            float v = curve(s, x1, x2);
            if (Math.abs(v - x) < 1e-5f) return s;
            if (v < x) lo = s;
            else hi = s;
            s = (lo + hi) * 0.5f;
        }
        return s;
    }

    private static float curve(float s, float p1, float p2) {
        float u = 1f - s;
        return 3f * u * u * s * p1 + 3f * u * s * s * p2 + s * s * s;
    }

    private static float slope(float s, float p1, float p2) {
        float u = 1f - s;
        return 3f * u * u * p1 + 6f * u * s * (p2 - p1) + 3f * s * s * (1f - p2);
    }
}
