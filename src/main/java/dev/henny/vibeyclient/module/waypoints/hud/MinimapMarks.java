package dev.henny.vibeyclient.module.waypoints.hud;

import dev.henny.vibeyclient.module.waypoints.core.map.MinimapMath;
import dev.henny.vibeyclient.ui.Gfx;

final class MinimapMarks {
    static final String ARROW = "▲";
    static final String NORTH = "N";

    private MinimapMarks() {}

    static void arrow(Gfx g, float x, float y, float angle) {
        g.push();
        g.translate(x, y);
        g.rotate(angle);
        int aw = g.width(ARROW);
        g.text(ARROW, -aw / 2, -4, 0xFFFFFFFF);
        g.pop();
    }

    static void north(Gfx g, float rot, int w, int h, float[] pt) {
        MinimapMath.project(0, -10_000, rot, 1, Math.min(w, h) / 2.0 - 6, pt);
        int nw = g.width(NORTH);
        g.text(NORTH, Math.round(w / 2f + pt[0]) - nw / 2, Math.round(h / 2f + pt[1]) - 4, 0xFFFF5A5A);
    }
}
