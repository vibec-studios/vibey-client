package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.module.waypoints.core.Pos;
import dev.henny.vibeyclient.module.waypoints.core.Waypoint;
import dev.henny.vibeyclient.module.waypoints.data.StoreFiles;
import dev.henny.vibeyclient.module.waypoints.data.WorldContext;
import dev.henny.vibeyclient.module.waypoints.hud.PortalLook;
import dev.henny.vibeyclient.module.waypoints.hud.WaypointHud;
import dev.henny.vibeyclient.module.waypoints.map.ScanStats;
import dev.henny.vibeyclient.module.waypoints.screen.WorldMapScreen;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.DeathScreen;
import org.joml.Vector3d;

import java.nio.file.Files;
import java.util.List;

import static dev.henny.vibeyclient.gametest.WaypointsTestKit.*;

final class WaypointsMapChecks {
    private WaypointsMapChecks() {}

    static void minimap(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> {
            mc.player.setYRot(200f);
            mc.player.setXRot(20f);
        });
        for (int i = 0; i < 40 && ctx.computeOnClient(mc -> WaypointHud.minimapTiles()) == 0; i++) ctx.waitTicks(5);
        int tiles = ctx.computeOnClient(mc -> WaypointHud.minimapTiles());
        check(tiles > 0, "the minimap drew no map tiles");
        ctx.waitTicks(40);
        shot(ctx, "07_minimap");
    }

    static void worldMap(ClientGameTestContext ctx) {
        ctx.getInput().pressKey(KEY_M);
        ctx.waitTicks(10);
        check(ctx.computeOnClient(mc -> mc.screen instanceof WorldMapScreen), "M should open the world map");
        for (int i = 0; i < 40 && ctx.computeOnClient(mc -> ((WorldMapScreen) mc.screen).lastTiles()) == 0; i++) ctx.waitTicks(5);
        check(ctx.computeOnClient(mc -> ((WorldMapScreen) mc.screen).lastTiles()) > 0, "the world map drew no tiles");
        String errors = ctx.computeOnClient(mc -> ((WorldMapScreen) mc.screen).document().diagnostics().format());
        check(errors.isBlank(), "map document has errors: " + errors);
        shot(ctx, "08_world_map");
        int count = ctx.computeOnClient(mc -> rt().store().list().size());
        ctx.getInput().setCursorPos(640 + 120, 360 + 60);
        ctx.waitTicks(2);
        ctx.getInput().pressMouse(LEFT);
        ctx.waitTicks(4);
        String placed = ctx.computeOnClient(mc -> ((WorldMapScreen) mc.screen).input().lastPlaced());
        check(placed != null && ctx.computeOnClient(mc -> rt().store().list().size()) == count + 1, "click on the map should place a waypoint");
        ctx.getInput().setCursorPos(640 + 120, 360 + 60);
        ctx.waitTicks(2);
        shot(ctx, "09_map_placed");
        double cx = ctx.computeOnClient(mc -> ((WorldMapScreen) mc.screen).mapView().centerX());
        ctx.getInput().setCursorPos(400, 400);
        ctx.getInput().holdMouse(LEFT);
        for (int i = 1; i <= 6; i++) {
            ctx.getInput().setCursorPos(400 + i * 20, 400);
            ctx.waitTicks(1);
        }
        ctx.getInput().releaseMouse(LEFT);
        ctx.waitTicks(2);
        double cx2 = ctx.computeOnClient(mc -> ((WorldMapScreen) mc.screen).mapView().centerX());
        check(cx2 < cx - 10, "dragging the map should pan it, center " + cx + " -> " + cx2);
        double z = ctx.computeOnClient(mc -> ((WorldMapScreen) mc.screen).mapView().zoom());
        ctx.getInput().scroll(1);
        ctx.waitTicks(3);
        double z2 = ctx.computeOnClient(mc -> ((WorldMapScreen) mc.screen).mapView().zoom());
        check(z2 > z, "the wheel should zoom the map in, " + z + " -> " + z2);
        shot(ctx, "10_map_panned_zoomed");
        ctx.getInput().pressKey(ESCAPE);
        ctx.waitTicks(4);
    }

    static void shareRoundTrip(ClientGameTestContext ctx) {
        List<String> before = ctx.computeOnClient(mc -> rt().store().list().all().stream().map(Waypoint::id).toList());
        int added = ctx.computeOnClient(mc -> {
            rt().actions().copy(rt().store().list().all());
            return rt().actions().importClipboard();
        });
        check(added == before.size(), "importing the copied code should add " + before.size() + ", added " + added);
        ctx.runOnClient(mc -> rt().store().list().all().stream().map(Waypoint::id).filter(id -> !before.contains(id))
                .toList().forEach(id -> rt().actions().remove(id)));
    }

    static void portal(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        int x = o[0] - 4, y = o[1], z = o[2] - 1;
        cmd(world, "fill %d %d %d %d %d %d minecraft:obsidian", x, y, z, x, y + 4, z + 3);
        cmd(world, "fill %d %d %d %d %d %d minecraft:nether_portal[axis=z]", x, y + 1, z + 1, x, y + 3, z + 2);
        ctx.waitTicks(10);
        lookAt(ctx, new Vector3d(x + 0.5, y + 2, z + 2));
        String text = ctx.computeOnClient(mc -> PortalLook.text());
        log("portal: %s", text);
        String expected = Math.floorDiv(x, 8) + " " + Math.floorDiv(z + 2, 8);
        check(text.endsWith(expected), "looking at the portal should show nether " + expected + ", got '" + text + "'");
        shot(ctx, "11_portal_coords");
    }

    static void death(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        cmd(world, "tp @a %.1f %d %.1f 0 0", o[0] + 2.5, o[1], o[2] + 3.5);
        ctx.waitTicks(10);
        cmd(world, "kill @a");
        ctx.waitTicks(20);
        check(ctx.computeOnClient(mc -> mc.screen instanceof DeathScreen), "kill should show the death screen");
        Waypoint death = ctx.computeOnClient(mc -> rt().store().list().death());
        check(death != null, "dying should add a death marker");
        Pos p = death.pos();
        check(Math.abs(p.x() - (o[0] + 2)) <= 1 && Math.abs(p.z() - (o[2] + 3)) <= 1, "death marker at the wrong place: " + p);
        ctx.runOnClient(mc -> mc.player.respawn());
        ctx.waitTicks(30);
        ctx.runOnClient(mc -> mc.setScreen(null));
        cmd(world, "tp @a %.1f %d %.1f 0 0", o[0] - 1.5, o[1], o[2] + 8.5);
        ctx.waitTicks(20);
        String id = death.id();
        lookAt(ctx, beaconCenter(ctx, id));
        check(hovering(ctx, id), "looking at the death marker should select it");
        ctx.getInput().pressMouse(LEFT);
        ctx.waitTicks(3);
        shot(ctx, "12_death_marker");
    }

    static void saved(ClientGameTestContext ctx) {
        ctx.waitTicks(30);
        boolean exists = ctx.computeOnClient(mc -> Files.isRegularFile(StoreFiles.waypoints(WorldContext.worldKey(mc))));
        check(exists, "the waypoints file was not written");
    }

    static void perf(ClientGameTestContext ctx) {
        String s = ctx.computeOnClient(mc -> {
            ScanStats st = rt().map().scanStats();
            return String.format(java.util.Locale.ROOT,
                    "scan: %d chunks, %.3f ms/tick avg, %.3f ms worst tick, %.3f ms/chunk | minimap paint %.1f us | tiles in memory %d, textures %d, uploads %d | fps %d",
                    st.chunks(), st.averageMillisPerTick(), st.worstMillis(), st.millisPerChunk(),
                    WaypointHud.minimapPaintNanos() / 1000.0, rt().map().loadedTiles(), rt().map().textures().count(),
                    rt().map().textures().uploads(), mc.getFps());
        });
        log("perf %s", s);
    }
}
