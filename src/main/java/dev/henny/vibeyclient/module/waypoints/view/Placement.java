package dev.henny.vibeyclient.module.waypoints.view;

import dev.henny.vibeyclient.module.waypoints.core.NetherCoords;
import dev.henny.vibeyclient.module.waypoints.core.Pos;
import dev.henny.vibeyclient.module.waypoints.core.Waypoint;

public final class Placement {
    private Placement() {}

    public static Pos shownPos(Waypoint w, String dimension) {
        if (dimension == null) return null;
        if (w.dimension().equals(dimension)) return w.pos();
        return NetherCoords.convert(w.pos(), w.dimension(), dimension);
    }

    public static boolean foreign(Waypoint w, String dimension) {
        return !w.dimension().equals(dimension);
    }
}
