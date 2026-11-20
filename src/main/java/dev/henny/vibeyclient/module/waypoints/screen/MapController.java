package dev.henny.vibeyclient.module.waypoints.screen;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.module.waypoints.core.map.MapView;
import dev.henny.vibeyclient.module.waypoints.view.CoordText;
import dev.henny.vui.bind.State;

public final class MapController {
    static final double STEP = 1.5;

    public final State<String> cursor = new State<>("");
    public final State<String> zoomText = new State<>("");
    public final State<String> dimName = new State<>("");

    private final MapView view;
    private final Runnable centerOnPlayer;

    MapController(MapView view, Runnable centerOnPlayer) {
        this.view = view;
        this.centerOnPlayer = centerOnPlayer;
    }

    public String t(String key) {
        return Lang.tr(key);
    }

    void update(String dimension, double mouseX, double mouseY) {
        dimName.set(dimension == null ? "" : CoordText.dimensionName(dimension));
        cursor.set((int) Math.floor(view.worldX(mouseX)) + " " + (int) Math.floor(view.worldZ(mouseY)));
        double z = view.zoom();
        zoomText.set((z >= 1 ? String.valueOf(Math.round(z * 10) / 10.0) : "1/" + Math.round(1 / z)) + "×");
    }

    public void center() {
        centerOnPlayer.run();
    }

    public void zoomIn() {
        view.zoom(view.zoom() * STEP);
    }

    public void zoomOut() {
        view.zoom(view.zoom() / STEP);
    }

    public void list() {
        Screens.list(null);
    }

    public void close() {
        Screens.closeCurrent();
    }
}
