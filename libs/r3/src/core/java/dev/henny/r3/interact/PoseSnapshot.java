package dev.henny.r3.interact;

import dev.henny.r3.anchor.Anchor;
import dev.henny.r3.object.Transform;

public record PoseSnapshot(Transform transform, Anchor anchor) {
    public static PoseSnapshot capture(Interactable target) {
        Anchor a = target.anchor();
        return new PoseSnapshot(target.transform().copy(), a == null ? null : a.copy());
    }

    public void restore(Interactable target) {
        target.transform().set(transform);
        if (anchor != null) target.anchor(anchor.copy());
    }

    public boolean differsFrom(Interactable target) {
        return !transform.sameAs(target.transform(), 1e-5) || anchor != null && target.anchor() != null
                && anchor.kind() != target.anchor().kind();
    }
}
