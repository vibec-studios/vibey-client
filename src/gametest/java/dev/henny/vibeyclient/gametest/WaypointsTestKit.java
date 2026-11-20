package dev.henny.vibeyclient.gametest;

import dev.henny.r3.mc.R3Mc;
import dev.henny.r3.object.WorldObject;
import dev.henny.vibeyclient.module.waypoints.WaypointsModule;
import dev.henny.vibeyclient.module.waypoints.WaypointsRuntime;
import dev.henny.vibeyclient.module.waypoints.core.Waypoint;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import org.joml.Vector3d;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

final class WaypointsTestKit {
    static final String P = "[waypoints-gametest] ";
    static final int LEFT = 0, RIGHT = 1, MIDDLE = 2, ESCAPE = 256, KEY_J = 74, KEY_M = 77;

    private WaypointsTestKit() {}

    static WaypointsRuntime rt() {
        return WaypointsModule.get().runtime();
    }

    static Waypoint waypoint(ClientGameTestContext ctx, String id) {
        return ctx.computeOnClient(mc -> rt().store().list().get(id));
    }

    static WorldObject beacon(String id) {
        return rt().beacons().object(id);
    }

    static Vector3d beaconCenter(ClientGameTestContext ctx, String id) {
        return ctx.computeOnClient(mc -> new Vector3d(beacon(id).transform().position()));
    }

    static boolean hovering(ClientGameTestContext ctx, String id) {
        return ctx.computeOnClient(mc -> {
            WorldObject o = beacon(id);
            return o != null && R3Mc.scene().interaction().hover().target() == o;
        });
    }

    static void cmd(TestSingleplayerContext world, String format, Object... args) {
        world.getServer().runCommand(String.format(Locale.ROOT, format, args));
    }

    static void lookAt(ClientGameTestContext ctx, Vector3d target) {
        float[] r = ctx.computeOnClient(mc -> {
            var e = mc.player.getEyePosition();
            double dx = target.x - e.x, dy = target.y - e.y, dz = target.z - e.z;
            return new float[] {(float) Math.toDegrees(Math.atan2(-dx, dz)),
                    (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)))};
        });
        ctx.runOnClient(mc -> {
            mc.player.setYRot(r[0]);
            mc.player.setXRot(r[1]);
            mc.player.yRotO = r[0];
            mc.player.xRotO = r[1];
            mc.player.setYHeadRot(r[0]);
        });
        ctx.waitTicks(4);
    }

    static void turn(ClientGameTestContext ctx, Vector3d from, Vector3d to, int steps) {
        for (int i = 1; i <= steps; i++) lookAt(ctx, new Vector3d(from).lerp(to, i / (double) steps));
    }

    static void shot(ClientGameTestContext ctx, String name) {
        ctx.runOnClient(mc -> mc.gui.getChat().clearMessages(false));
        ctx.waitTicks(2);
        Path file = ctx.takeScreenshot("waypoints_" + name);
        Path review = root().resolve("build/review/waypoints");
        try {
            Files.createDirectories(review);
            Files.copy(file, review.resolve(name + ".png"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new AssertionError("copy screenshot " + name, e);
        }
    }

    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(P + message);
    }

    static void log(String format, Object... args) {
        System.out.println(P + String.format(Locale.ROOT, format, args));
    }

    static Path root() {
        Path p = Path.of("").toAbsolutePath();
        while (p != null && !Files.exists(p.resolve("stonecutter.gradle"))) p = p.getParent();
        return p == null ? Path.of("").toAbsolutePath() : p;
    }
}
