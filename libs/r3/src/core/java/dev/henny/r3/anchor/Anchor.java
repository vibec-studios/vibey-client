package dev.henny.r3.anchor;

import dev.henny.r3.object.Transform;

public interface Anchor {
    AnchorKind kind();

    boolean resolve(AnchorContext context, Transform target);

    Anchor copy();

    default boolean followsSomething() {
        return false;
    }
}
