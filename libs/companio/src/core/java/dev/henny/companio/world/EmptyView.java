package dev.henny.companio.world;

import dev.henny.companio.move.BlockView;
import dev.henny.companio.move.Fluid;
import dev.henny.companio.move.Surface;

final class EmptyView implements BlockView {
    static final EmptyView INSTANCE = new EmptyView();

    public float top(int x, int y, int z) {
        return 0f;
    }

    public Fluid fluid(int x, int y, int z) {
        return Fluid.NONE;
    }

    public Surface surface(int x, int y, int z) {
        return Surface.NORMAL;
    }

    public boolean hazard(int x, int y, int z) {
        return false;
    }

    public boolean loaded(int x, int z) {
        return true;
    }
}
