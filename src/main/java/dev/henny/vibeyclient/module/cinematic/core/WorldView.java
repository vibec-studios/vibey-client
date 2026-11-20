package dev.henny.vibeyclient.module.cinematic.core;

public interface WorldView {
    int SOLID = 1, OPAQUE = 2, BUILT = 4, LIGHT = 8, WATER = 16, NATURAL = 32;

    int tags(int x, int y, int z);

    int kind(int x, int y, int z);

    int surface(int x, int z);

    boolean loaded(int x, int z);

    int minY();

    int maxY();

    default boolean solid(int x, int y, int z) {
        return (tags(x, y, z) & SOLID) != 0;
    }

    default boolean opaque(int x, int y, int z) {
        return (tags(x, y, z) & OPAQUE) != 0;
    }
}
