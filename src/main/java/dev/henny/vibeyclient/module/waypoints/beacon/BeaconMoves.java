package dev.henny.vibeyclient.module.waypoints.beacon;

import dev.henny.r3.anchor.BlockFaceAnchor;
import dev.henny.r3.object.WorldObject;
import dev.henny.vibeyclient.module.waypoints.core.Pos;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.ArrayDeque;
import java.util.Deque;

final class BeaconMoves {
    private record Entry(Pos pos, Vector3d shownAt) {}

    private static final int DEPTH = 16;

    private final Deque<Entry> history = new ArrayDeque<>();
    private Entry pending;

    static Pos target(WorldObject o) {
        if (o.anchor() instanceof BlockFaceAnchor a) {
            return Pos.clamped((long) a.blockX() + a.face().stepX(), (long) a.blockY() + a.face().stepY(),
                    (long) a.blockZ() + a.face().stepZ());
        }
        Vector3dc p = o.transform().position();
        return Pos.clamped((long) Math.floor(p.x()), (long) Math.floor(p.y() - BeaconAnchor.LIFT), (long) Math.floor(p.z()));
    }

    void grabbed(Pos before, Vector3dc shownAt) {
        pending = new Entry(before, new Vector3d(shownAt));
    }

    void committed() {
        if (pending == null) return;
        history.push(pending);
        while (history.size() > DEPTH) history.removeLast();
        pending = null;
    }

    boolean undoMatches(Vector3dc restored) {
        Entry top = history.peek();
        return top != null && top.shownAt().distance(restored.x(), restored.y(), restored.z()) < 1e-3;
    }

    Pos pop() {
        return history.pop().pos();
    }
}
