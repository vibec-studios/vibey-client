package dev.henny.companio.move;

import java.util.Random;

final class Water {
    private Water() {}

    static int[] pick(BlockView v, Body b, int radius, Random random) {
        int bx = Collide.floor(b.x), by = Collide.floor(b.y + 0.2), bz = Collide.floor(b.z);
        int[] best = null;
        double bestScore = Double.MAX_VALUE;
        for (int i = 0; i < 48; i++) {
            int x = bx + random.nextInt(radius * 2 + 1) - radius, z = bz + random.nextInt(radius * 2 + 1) - radius;
            for (int y = by + 2; y >= by - 3; y--) {
                if (v.fluid(x, y, z) != Fluid.WATER || v.fluid(x, y + 1, z) == Fluid.WATER || v.top(x, y + 1, z) > 0) continue;
                double d = Math.hypot(x - bx, z - bz) + random.nextDouble() * 3;
                if (b.inWater) d = -d;
                if (d < bestScore) {
                    bestScore = d;
                    best = new int[] {x, y, z};
                }
                break;
            }
        }
        return best;
    }
}
