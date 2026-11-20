package dev.henny.vibeyclient.gametest;

import dev.henny.companio.mc.CompanioMc;
import dev.henny.companio.world.Companion;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import org.joml.Vector3d;

import java.util.Locale;

final class CompanionTestKit {
    static final String P = "[companion-gametest] ";
    static final int LEFT = 0, RIGHT = 1, MIDDLE = 2, ESCAPE = 256;
    static final ReviewShots SHOTS = new ReviewShots("companion");

    private CompanionTestKit() {}

    static void cmd(TestSingleplayerContext world, String format, Object... args) {
        world.getServer().runCommand(args.length == 0 ? format : String.format(Locale.ROOT, format, args));
    }

    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    static void log(String format, Object... args) {
        System.out.println(P + String.format(Locale.ROOT, format, args));
    }

    static void shot(ClientGameTestContext ctx, String name) {
        SHOTS.shot(ctx, name);
    }

    static Companion own(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> CompanioMc.own());
    }

    static Vector3d center(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> {
            Companion c = CompanioMc.own();
            return new Vector3d(c.body.x, c.body.y + c.body.height * 0.5, c.body.z);
        });
    }

    static String behavior(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> {
            Companion c = CompanioMc.own();
            return c == null || c.brain().current() == null ? "-" : c.brain().current().def.id();
        });
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
            mc.player.yBodyRot = r[0];
        });
        ctx.waitTicks(3);
    }

    static void tp(TestSingleplayerContext world, ClientGameTestContext ctx, double x, double y, double z, float yaw, float pitch) {
        cmd(world, "tp @a %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch);
        ctx.waitTicks(3);
    }

    static void place(ClientGameTestContext ctx, double x, double y, double z) {
        ctx.runOnClient(mc -> CompanioMc.own().place(x, y, z));
    }

    static void waitFor(ClientGameTestContext ctx, String what, int maxTicks, java.util.function.BooleanSupplier ok) {
        for (int i = 0; i < maxTicks; i += 5) {
            if (ctx.computeOnClient(mc -> ok.getAsBoolean())) return;
            ctx.waitTicks(5);
        }
        throw new AssertionError("timed out waiting for " + what + " (behavior " + behavior(ctx) + ")");
    }
}
