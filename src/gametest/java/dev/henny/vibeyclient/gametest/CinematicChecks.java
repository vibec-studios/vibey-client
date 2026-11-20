package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.module.cinematic.CinematicModule;
import dev.henny.vibeyclient.module.cinematic.core.plan.Mode;
import dev.henny.vibeyclient.module.cinematic.run.Cinematic;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

import static dev.henny.vibeyclient.gametest.CinematicGameTest.KIT;
import static dev.henny.vibeyclient.gametest.ReviewKit.check;

final class CinematicChecks {
    private CinematicChecks() {}

    static CinematicModule m() {
        return CinematicModule.get();
    }

    static double[] player(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> new double[] {mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot()});
    }

    static void samePlayer(double[] a, double[] b, String where) {
        for (int i = 0; i < a.length; i++) check(Math.abs(a[i] - b[i]) < 1e-4, "player moved/turned during " + where + " (" + i + ")");
    }

    static void modes(ClientGameTestContext ctx) {
        double[] before = player(ctx);
        for (Mode mode : Mode.values()) {
            ctx.runOnClient(mc -> m().mode.set(mode));
            ctx.runOnClient(mc -> check(Cinematic.start(false), "start " + mode));
            ctx.waitTicks(mode == Mode.PANORAMA || mode == Mode.SKY ? 110 : 80);
            String info = ctx.computeOnClient(mc -> camera(mc, mode));
            KIT.log("mode %s: %s", mode, info);
            KIT.shot(ctx, "mode_" + mode.name().toLowerCase(Locale.ROOT));
            ctx.runOnClient(mc -> Cinematic.abort());
            ctx.waitTicks(3);
        }
        samePlayer(before, player(ctx), "modes");
    }

    private static String camera(net.minecraft.client.Minecraft mc, Mode mode) {
        check(Cinematic.active(), mode + " not running");
        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        Vec3 eye = mc.player.getEyePosition();
        double dist = cam.distanceTo(eye);
        int radius = m().radius.get();
        check(dist <= radius + 0.01, mode + " camera too far: " + dist);
        BlockPos at = BlockPos.containing(cam);
        check(mc.level.getBlockState(at).getCollisionShape(mc.level, at).isEmpty(), mode + " camera inside a block at " + at);
        var d = Cinematic.director();
        return String.format(Locale.ROOT, "playing %s subject '%s' dist %.1f fade %.2f", d.playing(), d.subject(), dist,
                Cinematic.frame() == null ? -1 : Cinematic.frame().fade());
    }

    static void watchFairness(ClientGameTestContext ctx, Mode mode, int ticks) {
        ctx.runOnClient(mc -> m().mode.set(mode));
        ctx.runOnClient(mc -> Cinematic.start(false));
        for (int i = 0; i < ticks; i += 5) {
            ctx.waitTicks(5);
            ctx.runOnClient(mc -> camera(mc, mode));
        }
        ctx.runOnClient(mc -> Cinematic.abort());
        KIT.log("fairness %s: %d ticks clear", mode, ticks);
    }
}
