package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.module.waypoints.WaypointsModule;
import dev.henny.vibeyclient.module.waypoints.core.Pos;
import dev.henny.vibeyclient.module.waypoints.core.Waypoint;
import dev.henny.vibeyclient.module.waypoints.data.WorldContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import org.joml.Vector3d;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static dev.henny.vibeyclient.gametest.WaypointsTestKit.check;
import static dev.henny.vibeyclient.gametest.WaypointsTestKit.cmd;
import static dev.henny.vibeyclient.gametest.WaypointsTestKit.log;
import static dev.henny.vibeyclient.gametest.WaypointsTestKit.lookAt;
import static dev.henny.vibeyclient.gametest.WaypointsTestKit.rt;
import static dev.henny.vibeyclient.gametest.WaypointsTestKit.shot;

final class WaypointsVisualChecks {
    private static final int[][] CLUSTER = {{2, 0, -70}, {3, 0, -72}, {1, 1, -74}};

    private WaypointsVisualChecks() {}

    static void run(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        List<String> ids = cluster(ctx, o);
        ctx.waitTicks(10);
        lookAt(ctx, new Vector3d(o[0] + 2.5, o[1] + 2.5, o[2] - 72));
        ctx.waitTicks(6);
        Set<Integer> levels = new HashSet<>();
        for (String id : ids) levels.add(ctx.computeOnClient(mc -> rt().beacons().labelLevel(id)));
        log("cluster label levels %s", levels);
        check(levels.size() == ids.size(), "close beacons should stack their labels, levels " + levels);
        shot(ctx, "07_declutter");
        cmd(world, "fill %d %d %d %d %d %d minecraft:stone", o[0] - 8, o[1], o[2] - 12, o[0] + 8, o[1] + 14, o[2] - 12);
        ctx.waitTicks(10);
        world.getClientWorld().waitForChunksRender();
        shot(ctx, "08_behind_wall");
        ctx.runOnClient(mc -> WaypointsModule.get().beamsThrough.set(true));
        ctx.waitTicks(4);
        shot(ctx, "09_beam_through_wall");
        ctx.runOnClient(mc -> WaypointsModule.get().beamsThrough.set(false));
        cmd(world, "fill %d %d %d %d %d %d minecraft:air", o[0] - 8, o[1], o[2] - 12, o[0] + 8, o[1] + 14, o[2] - 12);
        ctx.runOnClient(mc -> ids.forEach(rt().actions()::remove));
        ctx.waitTicks(4);
    }

    private static List<String> cluster(ClientGameTestContext ctx, int[] o) {
        return ctx.computeOnClient(mc -> {
            String dim = WorldContext.dimension(mc.level);
            String[] names = {"Mine A", "Mine B", "Mine C"};
            int[] colors = {0x4DA3FF, 0xFF5C5C, 0x3FE0C5};
            java.util.ArrayList<String> out = new java.util.ArrayList<>();
            for (int i = 0; i < CLUSTER.length; i++) {
                Waypoint w = rt().actions().add(new Pos(o[0] + CLUSTER[i][0], o[1] + CLUSTER[i][1], o[2] + CLUSTER[i][2]), dim);
                rt().actions().rename(w, names[i]);
                rt().actions().color(w, colors[i]);
                out.add(w.id());
            }
            return out;
        });
    }
}
