package dev.henny.vibeyclient.gametest;

import dev.henny.r3.interact.InteractionState;
import dev.henny.r3.mc.R3Mc;
import dev.henny.vibeyclient.module.waypoints.WaypointsModule;
import dev.henny.vibeyclient.module.waypoints.core.Pos;
import dev.henny.vibeyclient.module.waypoints.core.Waypoint;
import dev.henny.vibeyclient.module.waypoints.data.WorldContext;
import dev.henny.vibeyclient.module.waypoints.screen.ListScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.level.levelgen.Heightmap;
import org.joml.Vector3d;

import static dev.henny.vibeyclient.gametest.WaypointsTestKit.*;

public final class WaypointsGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        ctx.runOnClient(mc -> {
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
        });
        try (TestSingleplayerContext world = ctx.worldBuilder().setUseConsistentSettings(false)
                .adjustSettings(s -> s.setSeed("vibey-waypoints")).create()) {
            world.getClientWorld().waitForChunksRender();
            int[] o = setup(ctx, world);
            addWithKey(ctx);
            String tower = addAhead(ctx, o);
            hoverAndNavigate(ctx, tower, o);
            middleDrag(ctx, tower, o);
            wheel(ctx, tower);
            contextMenu(ctx, tower);
            WaypointsVisualChecks.run(ctx, world, o);
            WaypointsMapChecks.minimap(ctx);
            WaypointsMapChecks.worldMap(ctx);
            WaypointsMapChecks.shareRoundTrip(ctx);
            WaypointsMapChecks.portal(ctx, world, o);
            WaypointsMapChecks.death(ctx, world, o);
            WaypointsMapChecks.saved(ctx);
            WaypointsMapChecks.perf(ctx);
        }
    }

    private static int[] setup(ClientGameTestContext ctx, TestSingleplayerContext world) {
        for (String c : new String[] {"gamemode creative @a", "time set 6000", "weather clear", "gamerule fallDamage false",
                "gamerule doDaylightCycle false", "difficulty peaceful"}) world.getServer().runCommand(c);
        int[] p = ctx.computeOnClient(mc -> {
            int x = (int) Math.floor(mc.player.getX()), z = (int) Math.floor(mc.player.getZ());
            return new int[] {x, mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z};
        });
        int x = p[0], y = Math.max(p[1], 64), z = p[2];
        cmd(world, "fill %d %d %d %d %d %d minecraft:air", x - 8, y, z - 10, x + 8, y + 12, z + 8);
        cmd(world, "fill %d %d %d %d %d %d minecraft:grass_block", x - 8, y - 1, z - 10, x + 8, y - 1, z + 8);
        cmd(world, "setworldspawn %d %d %d", x, y, z);
        cmd(world, "tp @a %.1f %d %.1f 180 0", x + 0.5, y, z + 0.5);
        ctx.runOnClient(mc -> {
            WaypointsModule m = WaypointsModule.get();
            m.minimap.set(true);
            m.minimapRotate.set(true);
            m.minimapZoom.set(2);
        });
        ctx.waitTicks(40);
        world.getClientWorld().waitForChunksRender();
        log("scene at %d %d %d", x, y, z);
        return new int[] {x, y, z};
    }

    private static void addWithKey(ClientGameTestContext ctx) {
        ctx.getInput().pressKey(KEY_J);
        ctx.waitTicks(4);
        int n = ctx.computeOnClient(mc -> rt().store().list().size());
        check(n == 1, "J should add one waypoint, list has " + n);
        ctx.runOnClient(mc -> {
            Waypoint w = rt().store().list().all().get(0);
            rt().actions().rename(w, "Home");
        });
    }

    private static String addAhead(ClientGameTestContext ctx, int[] o) {
        String id = ctx.computeOnClient(mc -> {
            Waypoint w = rt().actions().add(new Pos(o[0], o[1], o[2] - 6), WorldContext.dimension(mc.level));
            rt().actions().rename(w, "Tower");
            rt().actions().color(w, 0xFFE14D);
            return w.id();
        });
        ctx.waitTicks(5);
        check(ctx.computeOnClient(mc -> beacon(id) != null), "the tower waypoint got no beacon");
        return id;
    }

    private static void hoverAndNavigate(ClientGameTestContext ctx, String tower, int[] o) {
        lookAt(ctx, beaconCenter(ctx, tower));
        check(hovering(ctx, tower), "looking at the beacon should select it");
        shot(ctx, "01_beacon_selected");
        ctx.getInput().pressMouse(LEFT);
        ctx.waitTicks(3);
        String target = ctx.computeOnClient(mc -> rt().navigator().targetId());
        check(tower.equals(target), "left click on the beacon should navigate to it, target " + target);
        lookAt(ctx, new Vector3d(o[0] + 8, o[1] + 1.6, o[2] + 2));
        shot(ctx, "02_navigation_arrow");
    }

    private static void middleDrag(ClientGameTestContext ctx, String tower, int[] o) {
        Vector3d from = beaconCenter(ctx, tower);
        lookAt(ctx, from);
        ctx.getInput().holdMouse(MIDDLE);
        ctx.waitTicks(3);
        InteractionState state = ctx.computeOnClient(mc -> R3Mc.scene().interaction().state());
        check(state == InteractionState.HOLDING, "middle mouse on the beacon should grab it, state " + state);
        Vector3d to = new Vector3d(o[0] + 4.5, o[1] + 0.02, o[2] - 3.5);
        turn(ctx, from, to, 8);
        shot(ctx, "03_dragging");
        ctx.getInput().releaseMouse(MIDDLE);
        ctx.waitTicks(4);
        Pos p = waypoint(ctx, tower).pos();
        log("moved tower to %d %d %d", p.x(), p.y(), p.z());
        check(Math.abs(p.x() - (o[0] + 4)) <= 1 && Math.abs(p.z() - (o[2] - 4)) <= 1 && Math.abs(p.y() - o[1]) <= 1,
                "the beacon should land on the ground near " + (o[0] + 4) + " " + o[1] + " " + (o[2] - 4) + ", was " + p);
        lookAt(ctx, beaconCenter(ctx, tower));
        shot(ctx, "04_moved");
    }

    private static void wheel(ClientGameTestContext ctx, String tower) {
        lookAt(ctx, beaconCenter(ctx, tower));
        check(hovering(ctx, tower), "should hover the moved beacon");
        float before = waypoint(ctx, tower).size();
        ctx.getInput().scroll(1);
        ctx.waitTicks(2);
        ctx.getInput().scroll(1);
        ctx.waitTicks(4);
        float after = waypoint(ctx, tower).size();
        log("wheel: size %.2f -> %.2f", before, after);
        check(after > before * 1.3f, "two wheel notches should grow the beacon");
        lookAt(ctx, beaconCenter(ctx, tower));
        shot(ctx, "05_bigger");
    }

    private static void contextMenu(ClientGameTestContext ctx, String tower) {
        lookAt(ctx, beaconCenter(ctx, tower));
        ctx.getInput().pressMouse(RIGHT);
        ctx.waitTicks(6);
        String sel = ctx.computeOnClient(mc -> mc.screen instanceof ListScreen s ? s.controller().selId.peek() : null);
        check(tower.equals(sel), "right click should open the list with the beacon selected, got " + sel);
        String errors = ctx.computeOnClient(mc -> ((ListScreen) mc.screen).document().diagnostics().format());
        check(errors.isBlank(), "list document has errors: " + errors);
        WaypointsVuiKeys.check();
        shot(ctx, "06_list_editor");
        ctx.getInput().pressKey(ESCAPE);
        ctx.waitTicks(4);
        check(ctx.computeOnClient(mc -> mc.screen == null), "Esc should close the list");
    }
}
