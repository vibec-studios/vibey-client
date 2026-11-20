package dev.henny.companio.move;

public interface BlockView {
    float top(int x, int y, int z);

    Fluid fluid(int x, int y, int z);

    Surface surface(int x, int y, int z);

    boolean hazard(int x, int y, int z);

    boolean loaded(int x, int z);
}
