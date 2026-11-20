package dev.henny.vibeyclient.module.waypoints.hud;

import dev.henny.vibeyclient.module.waypoints.WaypointsRuntime;
import dev.henny.vibeyclient.module.waypoints.core.Pos;
import dev.henny.vibeyclient.module.waypoints.core.Waypoint;
import dev.henny.vibeyclient.module.waypoints.core.WaypointKind;
import dev.henny.vibeyclient.module.waypoints.core.map.MinimapMath;
import dev.henny.vibeyclient.module.waypoints.data.WorldContext;
import dev.henny.vibeyclient.module.waypoints.view.Placement;
import dev.henny.vibeyclient.ui.Gfx;

import java.util.ArrayList;
import java.util.List;

final class MinimapDots {
    private record Dot(Pos pos, int color, boolean death, boolean target) {}

    private final List<Dot> dots = new ArrayList<>();

    void refresh(WaypointsRuntime rt) {
        dots.clear();
        String dim = WorldContext.dimension();
        if (rt == null || !rt.active() || dim == null) return;
        String target = rt.navigator().targetId();
        for (Waypoint w : rt.store().list().all()) {
            if (!w.visible()) continue;
            Pos p = Placement.shownPos(w, dim);
            if (p != null) dots.add(new Dot(p, w.color(), w.kind() == WaypointKind.DEATH, w.id().equals(target)));
        }
    }

    void paint(Gfx g, double px, double pz, float rot, double zoom, int w, int h, float[] pt) {
        double half = Math.min(w, h) / 2.0 - 3;
        for (int i = 0; i < dots.size(); i++) {
            Dot d = dots.get(i);
            boolean inside = MinimapMath.project(d.pos.x() + 0.5 - px, d.pos.z() + 0.5 - pz, rot, zoom, half, pt);
            int x = Math.round(w / 2f + pt[0]), y = Math.round(h / 2f + pt[1]);
            int r = inside ? 2 : 1;
            g.rect(x - r - 1, y - r - 1, r * 2 + 2, r * 2 + 2, d.target ? 0xFFFFFFFF : 0xFF000000);
            g.rect(x - r, y - r, r * 2, r * 2, d.death ? 0xFFFF3B3B : 0xFF000000 | d.color);
        }
    }
}
