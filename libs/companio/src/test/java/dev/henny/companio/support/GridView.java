package dev.henny.companio.support;

import dev.henny.companio.move.BlockView;
import dev.henny.companio.move.Fluid;
import dev.henny.companio.move.Surface;

import java.util.HashMap;
import java.util.Map;

public final class GridView implements BlockView {
    private final Map<Long, Float> tops = new HashMap<>();
    private final Map<Long, Fluid> fluids = new HashMap<>();
    private final Map<Long, Surface> surfaces = new HashMap<>();
    private int floor = Integer.MIN_VALUE;

    public GridView floor(int y) {
        floor = y;
        return this;
    }

    public GridView solid(int x, int y, int z) {
        tops.put(key(x, y, z), 1f);
        return this;
    }

    public GridView top(int x, int y, int z, float top) {
        tops.put(key(x, y, z), top);
        return this;
    }

    public GridView air(int x, int y, int z) {
        tops.put(key(x, y, z), 0f);
        return this;
    }

    public GridView fluid(int x, int y, int z, Fluid f) {
        fluids.put(key(x, y, z), f);
        tops.put(key(x, y, z), 0f);
        return this;
    }

    public GridView surface(int x, int y, int z, Surface s) {
        surfaces.put(key(x, y, z), s);
        return this;
    }

    public GridView wall(int x, int z0, int z1, int y0, int y1) {
        for (int z = z0; z <= z1; z++) for (int y = y0; y <= y1; y++) solid(x, y, z);
        return this;
    }

    public float top(int x, int y, int z) {
        Float t = tops.get(key(x, y, z));
        if (t != null) return t;
        return y <= floor ? 1f : 0f;
    }

    public Fluid fluid(int x, int y, int z) {
        return fluids.getOrDefault(key(x, y, z), Fluid.NONE);
    }

    public Surface surface(int x, int y, int z) {
        return surfaces.getOrDefault(key(x, y, z), Surface.NORMAL);
    }

    public boolean hazard(int x, int y, int z) {
        return false;
    }

    public boolean loaded(int x, int z) {
        return Math.abs(x) < 200 && Math.abs(z) < 200;
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFF) << 42) | ((long) (y & 0xFFFFF) << 22) | (z & 0x3FFFFF);
    }
}
