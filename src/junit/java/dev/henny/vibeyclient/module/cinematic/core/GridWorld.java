package dev.henny.vibeyclient.module.cinematic.core;

import java.util.HashMap;
import java.util.Map;

public final class GridWorld implements WorldView {
    public static final int GRASS = 1, STONE = 2, PLANKS = 3, TORCH = 4, WATER_KIND = 5, GLASS = 6, BRICKS = 7;

    private final Map<Long, Integer> blocks = new HashMap<>();
    private final int floor;
    private final int loadedRadius;

    public GridWorld(int floor, int loadedRadius) {
        this.floor = floor;
        this.loadedRadius = loadedRadius;
    }

    public static GridWorld flat() {
        return new GridWorld(64, 200);
    }

    public GridWorld set(int x, int y, int z, int kind) {
        blocks.put(key(x, y, z), kind);
        return this;
    }

    public GridWorld fill(int x0, int y0, int z0, int x1, int y1, int z1, int kind) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, kind);
        return this;
    }

    public GridWorld box(int x0, int y0, int z0, int x1, int y1, int z1, int kind) {
        for (int x = x0; x <= x1; x++)
            for (int y = y0; y <= y1; y++)
                for (int z = z0; z <= z1; z++)
                    if (x == x0 || x == x1 || y == y0 || y == y1 || z == z0 || z == z1) set(x, y, z, kind);
        return this;
    }

    private int at(int x, int y, int z) {
        Integer k = blocks.get(key(x, y, z));
        if (k != null) return k;
        return y <= floor ? (y == floor ? GRASS : STONE) : 0;
    }

    @Override
    public int tags(int x, int y, int z) {
        return switch (at(x, y, z)) {
            case 0 -> 0;
            case GRASS, STONE -> SOLID | OPAQUE | NATURAL;
            case PLANKS, BRICKS -> SOLID | OPAQUE | BUILT;
            case GLASS -> SOLID | BUILT;
            case TORCH -> LIGHT | BUILT;
            case WATER_KIND -> WATER | NATURAL;
            default -> SOLID | OPAQUE;
        };
    }

    @Override
    public int kind(int x, int y, int z) {
        return at(x, y, z);
    }

    @Override
    public int surface(int x, int z) {
        for (int y = 140; y > minY(); y--) if (at(x, y, z) != 0) return y;
        return minY();
    }

    @Override
    public boolean loaded(int x, int z) {
        return Math.abs(x) <= loadedRadius && Math.abs(z) <= loadedRadius;
    }

    @Override
    public int minY() {
        return 0;
    }

    @Override
    public int maxY() {
        return 320;
    }

    private static long key(int x, int y, int z) {
        return ((long) x & 0x3FFFFFF) << 38 | ((long) z & 0x3FFFFFF) << 12 | (y & 0xFFF);
    }
}
