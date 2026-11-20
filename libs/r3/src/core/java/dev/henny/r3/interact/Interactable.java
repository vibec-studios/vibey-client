package dev.henny.r3.interact;

import dev.henny.r3.anchor.Anchor;
import dev.henny.r3.object.Transform;
import dev.henny.r3.panel.PointerContent;

public interface Interactable {
    Transform transform();

    InteractionRules rules();

    boolean locked();

    int pixelWidth();

    int pixelHeight();

    default PointerContent pointerContent() {
        return null;
    }

    default Anchor anchor() {
        return null;
    }

    default void anchor(Anchor anchor) {}

    default boolean hoverable() {
        return rules().interactive();
    }
}
