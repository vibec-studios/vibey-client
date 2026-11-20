package dev.henny.vibeyclient.module.waypoints.screen;

import dev.henny.vibeyclient.module.waypoints.WaypointsRuntime;
import dev.henny.vibeyclient.module.waypoints.core.Pos;
import dev.henny.vibeyclient.module.waypoints.core.Waypoint;
import dev.henny.vibeyclient.module.waypoints.core.WaypointKind;
import dev.henny.vibeyclient.module.waypoints.core.map.MapView;
import dev.henny.vibeyclient.module.waypoints.view.Placement;
import dev.henny.vibeyclient.ui.Gfx;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

final class MapMarkers {
    static final String DIAMOND = "◆";
    static final String SKULL = "☠";
    static final String ARROW = "▲";

    private MapMarkers() {}

    static void draw(Gfx g, MapView view, WaypointsRuntime rt, String dim, String hoveredId) {
        String nav = rt.navigator().targetId();
        boolean names = view.zoom() >= 2;
        for (Waypoint w : rt.store().list().all()) {
            Pos p = w.visible() ? Placement.shownPos(w, dim) : null;
            if (p == null) continue;
            int x = (int) Math.round(view.screenX(p.x() + 0.5)), y = (int) Math.round(view.screenY(p.z() + 0.5));
            if (x < -20 || y < -20 || x > g.screenWidth() + 20 || y > g.screenHeight() + 20) continue;
            boolean hover = w.id().equals(hoveredId);
            String mark = w.kind() == WaypointKind.DEATH ? SKULL : DIAMOND;
            int color = 0xFF000000 | w.color();
            if (hover || w.id().equals(nav)) g.outline(x - 6, y - 6, 12, 12, hover ? 0xFFFFFFFF : color);
            g.textShadow(mark, x - g.width(mark) / 2, y - 4, color);
            if (names || hover) {
                String label = Placement.foreign(w, dim) ? "⇄ " + w.name() : w.name();
                int lw = g.width(label);
                g.rect(x - lw / 2 - 2, y + 7, lw + 4, 11, 0xB0000000);
                g.text(label, x - lw / 2, y + 9, hover ? 0xFFFFFFFF : 0xFFD8D8E0);
            }
        }
        player(g, view);
    }

    private static void player(Gfx g, MapView view) {
        Player p = Minecraft.getInstance().player;
        if (p == null) return;
        g.push();
        g.translate((float) view.screenX(p.getX()), (float) view.screenY(p.getZ()));
        g.rotate((float) Math.toRadians(p.getYRot() + 180f));
        g.textShadow(ARROW, -g.width(ARROW) / 2, -4, 0xFFFFFFFF);
        g.pop();
    }
}
