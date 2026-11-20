package dev.henny.r3.anchor;

import dev.henny.r3.object.Transform;

public final class FreeAnchor implements Anchor {
    public static final FreeAnchor INSTANCE = new FreeAnchor();

    private FreeAnchor() {}

    @Override
    public AnchorKind kind() {
        return AnchorKind.FREE;
    }

    @Override
    public boolean resolve(AnchorContext context, Transform target) {
        return true;
    }

    @Override
    public Anchor copy() {
        return this;
    }
}
