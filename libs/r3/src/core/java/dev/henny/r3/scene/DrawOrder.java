package dev.henny.r3.scene;

import dev.henny.r3.object.WorldObject;

import java.util.Comparator;

final class DrawOrder implements Comparator<WorldObject> {
    static final DrawOrder INSTANCE = new DrawOrder();

    @Override
    public int compare(WorldObject a, WorldObject b) {
        if (a.layer() != b.layer()) return Integer.compare(a.layer(), b.layer());
        boolean ta = translucent(a);
        boolean tb = translucent(b);
        if (ta != tb) return ta ? 1 : -1;
        int byDistance = Double.compare(b.state().distance(), a.state().distance());
        return ta ? byDistance : -byDistance;
    }

    private static boolean translucent(WorldObject o) {
        return o.content() != null && o.content().translucent();
    }
}
