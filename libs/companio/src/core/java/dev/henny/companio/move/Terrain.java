package dev.henny.companio.move;

public final class Terrain {
    static final float LOW = 0.2f;
    public static final int MAX_DROP = 3;

    private final BlockView v;
    private final int headroom;
    private final boolean swims;

    public Terrain(BlockView v, float height, boolean swims) {
        this.v = v;
        this.headroom = Math.max(1, (int) Math.ceil(height - 0.05));
        this.swims = swims;
    }

    public BlockView view() {
        return v;
    }

    boolean passable(int x, int y, int z) {
        return v.top(x, y, z) <= LOW && v.fluid(x, y, z) != Fluid.LAVA && !v.hazard(x, y, z);
    }

    boolean clear(int x, int y, int z) {
        for (int k = 0; k < headroom; k++) if (!passable(x, y + k, z)) return false;
        return true;
    }

    public boolean standable(int x, int y, int z) {
        if (!v.loaded(x, z) || !clear(x, y, z)) return false;
        if (danger(x, y, z)) return false;
        Fluid here = v.fluid(x, y, z);
        if (here == Fluid.WATER) return swims;
        float below = v.top(x, y - 1, z);
        if (below > LOW) return v.fluid(x, y - 1, z) != Fluid.LAVA && !v.hazard(x, y - 1, z);
        return swims && v.fluid(x, y - 1, z) == Fluid.WATER;
    }

    boolean water(int x, int y, int z) {
        return v.fluid(x, y, z) == Fluid.WATER;
    }

    private boolean danger(int x, int y, int z) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (v.fluid(x + dx, y, z + dz) == Fluid.LAVA || v.fluid(x + dx, y - 1, z + dz) == Fluid.LAVA) return true;
            }
        }
        return v.hazard(x, y - 1, z);
    }

    public int dropTo(int x, int y, int z) {
        for (int d = 0; d <= MAX_DROP; d++) if (standable(x, y - d, z)) return y - d;
        return Integer.MIN_VALUE;
    }

    public int[] spotNear(double cx, double cy, double cz, int radius) {
        int bx = (int) Math.floor(cx), by = (int) Math.floor(cy + 0.01), bz = (int) Math.floor(cz);
        for (int r = 1; r <= radius; r++) {
            for (int dy = 0; dy <= 2; dy++) {
                for (int i = 0; i < 8 * r; i++) {
                    double a = Math.PI * 2 * i / (8.0 * r);
                    int x = bx + (int) Math.round(Math.cos(a) * r), z = bz + (int) Math.round(Math.sin(a) * r);
                    for (int sy : new int[] {by + dy, by - dy}) if (standable(x, sy, z) && v.fluid(x, sy, z) == Fluid.NONE) return new int[] {x, sy, z};
                }
            }
        }
        return null;
    }
}
