package dev.henny.cosmetico.skeleton;

import dev.henny.cosmetico.model.Animation;
import dev.henny.cosmetico.model.Event;
import dev.henny.cosmetico.model.EventType;

public final class Visibility {
    private Visibility() {}

    public static boolean at(Animation a, int bone, float t) {
        boolean v = true;
        for (Event e : a.events) {
            if (e.t > t + 1e-4f) break;
            if (e.type == EventType.VISIBILITY && e.bone == bone) v = e.visible;
        }
        return v;
    }
}
