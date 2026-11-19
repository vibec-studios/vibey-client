package dev.henny.cosmetico.sim;

import dev.henny.cosmetico.model.Bone;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Cube;

import java.util.Map;
import java.util.WeakHashMap;

public final class PetGeometry {
    private PetGeometry() {}

    private static final Map<Bundle, Double> HALF_WIDTH = new WeakHashMap<>();

    public static double halfWidth(Bundle b) {
        if (b == null || b.model == null) return 0.25;
        synchronized (HALF_WIDTH) {
            return HALF_WIDTH.computeIfAbsent(b, PetGeometry::measure);
        }
    }

    public static double side(Bundle b) {
        return Math.max(0.95, 0.3 + halfWidth(b) + 0.3);
    }

    private static double measure(Bundle k) {
        float m = 0;
        for (Bone bone : k.model.bones) {
            for (Cube c : bone.cubes) {
                if (Math.abs(c.to[1] - c.from[1]) < 2f || Math.abs(c.to[2] - c.from[2]) < 2f) continue;
                m = Math.max(m, Math.max(Math.abs(c.from[0]), Math.abs(c.to[0])) + c.inflate);
            }
        }
        float scale = k.pet == null ? 1f : k.pet.scale();
        return Math.min(1.5, Math.max(0.1, m * scale / 16.0));
    }
}
