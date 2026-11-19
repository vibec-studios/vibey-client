package dev.henny.r3.interact;

import dev.henny.r3.object.Transform;

public final class UniformResize {
    private UniformResize() {}

    public static boolean step(Transform t, float deltaWidth, SizeLimits limits) {
        float w = t.worldWidth();
        float h = t.worldHeight();
        float factor = limits.clampFactor((w + deltaWidth) / w, w, h);
        if (Math.abs(factor - 1f) < 1e-6f) return false;
        t.scale(t.scale() * factor);
        return true;
    }
}
