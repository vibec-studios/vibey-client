package dev.henny.vibeyclient.module.waypoints.screen;

import dev.henny.vibeyclient.module.waypoints.WaypointsRuntime;
import dev.henny.vibeyclient.module.waypoints.core.Pos;
import dev.henny.vibeyclient.module.waypoints.core.Waypoint;
import dev.henny.vibeyclient.module.waypoints.core.map.MapView;
import dev.henny.vibeyclient.module.waypoints.view.Placement;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;

public final class MapInput {
    static final double CLICK_SLOP = 3;
    static final double HIT_RADIUS = 6;
    static final int LEFT = 0, RIGHT = 1;

    private final WaypointsRuntime rt;
    private final MapView view;
    private boolean pressed;
    private boolean dragging;
    private int button;
    private double startX, startY, lastX, lastY;
    private String lastPlaced;

    MapInput(WaypointsRuntime rt, MapView view) {
        this.rt = rt;
        this.view = view;
    }

    boolean pressed() {
        return pressed;
    }

    public String lastPlaced() {
        return lastPlaced;
    }

    void down(double x, double y, int b) {
        pressed = true;
        dragging = false;
        button = b;
        startX = lastX = x;
        startY = lastY = y;
    }

    void move(double x, double y) {
        if (!dragging && Math.hypot(x - startX, y - startY) > CLICK_SLOP) dragging = true;
        if (dragging && button == LEFT) view.pan(x - lastX, y - lastY);
        lastX = x;
        lastY = y;
    }

    void up(double x, double y, int b) {
        pressed = false;
        if (dragging) {
            dragging = false;
            return;
        }
        Waypoint hit = hit(x, y);
        if (b == LEFT && hit != null) Screens.list(hit.id());
        else if (b == LEFT) place(x, y);
        else if (b == RIGHT && hit != null) rt.actions().toggleNavigate(hit);
    }

    String hovered(double x, double y) {
        if (pressed && dragging) return null;
        Waypoint w = hit(x, y);
        return w == null ? null : w.id();
    }

    private void place(double x, double y) {
        String dim = rt.map().dimension();
        int wx = (int) Math.floor(view.worldX(x)), wz = (int) Math.floor(view.worldZ(y));
        Waypoint w = rt.actions().add(Pos.clamped(wx, surfaceY(wx, wz), wz), dim);
        if (w != null) lastPlaced = w.id();
    }

    private int surfaceY(int wx, int wz) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level.getChunkSource().getChunk(wx >> 4, wz >> 4, ChunkStatus.FULL, false) != null) {
            return mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, wx, wz);
        }
        return mc.player.getBlockY();
    }

    private Waypoint hit(double x, double y) {
        String dim = rt.map().dimension();
        Waypoint best = null;
        double bestD = HIT_RADIUS;
        for (Waypoint w : rt.store().list().all()) {
            Pos p = w.visible() ? Placement.shownPos(w, dim) : null;
            if (p == null) continue;
            double d = Math.hypot(view.screenX(p.x() + 0.5) - x, view.screenY(p.z() + 0.5) - y);
            if (d <= bestD) {
                bestD = d;
                best = w;
            }
        }
        return best;
    }
}
