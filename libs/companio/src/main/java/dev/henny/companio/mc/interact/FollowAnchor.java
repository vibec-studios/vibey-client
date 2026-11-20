package dev.henny.companio.mc.interact;

import dev.henny.r3.anchor.Anchor;
import dev.henny.r3.anchor.AnchorContext;
import dev.henny.r3.anchor.AnchorKind;
import dev.henny.r3.object.Transform;

import java.util.function.Predicate;

final class FollowAnchor implements Anchor {
    private final Predicate<double[]> position;
    private final double[] p = new double[4];

    FollowAnchor(Predicate<double[]> position) {
        this.position = position;
    }

    @Override
    public AnchorKind kind() {
        return AnchorKind.ENTITY;
    }

    @Override
    public boolean resolve(AnchorContext context, Transform target) {
        if (!position.test(p)) return false;
        target.position(p[0], p[1], p[2]);
        return true;
    }

    @Override
    public Anchor copy() {
        return this;
    }

    @Override
    public boolean followsSomething() {
        return true;
    }
}
