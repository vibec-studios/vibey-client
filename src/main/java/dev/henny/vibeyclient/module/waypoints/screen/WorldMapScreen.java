package dev.henny.vibeyclient.module.waypoints.screen;

import dev.henny.vibeyclient.module.waypoints.WaypointsRuntime;
import dev.henny.vibeyclient.module.waypoints.core.map.MapView;
import dev.henny.vibeyclient.module.waypoints.map.MapPainter;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vui.mc.host.VuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

public final class WorldMapScreen extends VuiScreen {
    public static final String DOCUMENT = "vibeyclient:waypoints/map";
    private static final int BG = 0xFF060609;
    private static final double WHEEL_STEP = 1.25;

    private final WaypointsRuntime rt;
    private final MapView map;
    private final MapController controller;
    private final MapInput input;
    private boolean centered;
    private int lastTiles;

    WorldMapScreen(WaypointsRuntime rt) {
        this(rt, new MapView().zoom(2));
    }

    private WorldMapScreen(WaypointsRuntime rt, MapView view) {
        this(rt, view, new MapController(view, () -> centerOnPlayer(view)));
    }

    private WorldMapScreen(WaypointsRuntime rt, MapView view, MapController controller) {
        super(DOCUMENT, controller);
        this.rt = rt;
        this.map = view;
        this.controller = controller;
        this.input = new MapInput(rt, view);
    }

    public MapView mapView() {
        return map;
    }

    public MapInput input() {
        return input;
    }

    public int lastTiles() {
        return lastTiles;
    }

    private static void centerOnPlayer(MapView view) {
        Player p = Minecraft.getInstance().player;
        if (p != null) view.center(p.getX(), p.getZ());
    }

    @Override
    protected void draw(Object graphics, int mouseX, int mouseY, float delta) {
        map.size(width, height);
        if (!centered) {
            centerOnPlayer(map);
            centered = true;
        }
        Gfx g = new Gfx((GuiGraphics) graphics, Minecraft.getInstance().font);
        g.rect(0, 0, width, height, BG);
        String dim = rt.map().dimension();
        if (dim != null) {
            g.push();
            g.translate(width / 2f, height / 2f);
            lastTiles = MapPainter.draw(g, rt.map().textures(), dim, map.centerX(), map.centerZ(), map.zoom(), width / 2.0, height / 2.0);
            g.pop();
            MapMarkers.draw(g, map, rt, dim, input.hovered(mouseX, mouseY));
        }
        controller.update(dim, mouseX, mouseY);
        g.layer();
        super.draw(graphics, mouseX, mouseY, delta);
    }

    @Override
    protected boolean pointerDown(double x, double y, int button) {
        if (view().hits(x, y)) return super.pointerDown(x, y, button);
        input.down(x, y, button);
        return true;
    }

    @Override
    protected boolean pointerMove(double x, double y) {
        if (input.pressed()) {
            input.move(x, y);
            return true;
        }
        return super.pointerMove(x, y);
    }

    @Override
    protected boolean pointerUp(double x, double y, int button) {
        if (input.pressed()) {
            input.up(x, y, button);
            return true;
        }
        return super.pointerUp(x, y, button);
    }

    @Override
    protected boolean wheel(double x, double y, double dx, double dy) {
        if (view().hits(x, y)) return super.wheel(x, y, dx, dy);
        if (dy != 0) map.zoomAt(x, y, dy > 0 ? WHEEL_STEP : 1 / WHEEL_STEP);
        return true;
    }
}
