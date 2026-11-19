package dev.henny.r3.object;

import dev.henny.r3.anchor.Facing;

public interface ObjectContent {
    default void tick() {}

    default void update(float dt) {}

    default Facing facing() {
        return Facing.KEEP;
    }

    default boolean translucent() {
        return true;
    }

    default double boundingRadius(Transform transform) {
        return transform.boundingRadius();
    }
}
