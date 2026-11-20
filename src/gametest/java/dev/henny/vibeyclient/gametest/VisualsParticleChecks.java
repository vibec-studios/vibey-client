package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.module.visuals.particles.ParticleGate;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import org.joml.Vector3d;

import static dev.henny.vibeyclient.gametest.ReviewKit.check;
import static dev.henny.vibeyclient.gametest.ReviewKit.cmd;
import static dev.henny.vibeyclient.gametest.VisualsGameTest.KIT;
import static dev.henny.vibeyclient.gametest.VisualsGameTest.m;

final class VisualsParticleChecks {
    private static final int BURST = 300;
    private static final int BACKGROUND = 60;

    private VisualsParticleChecks() {}

    static void particles(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        ReviewKit.lookAt(ctx, new Vector3d(o[0] + 0.5, o[1] + 1.2, o[2] - 4));
        long[] none = burst(ctx, world, o, "flame", 0, "particles_0x_flame");
        long[] one = burst(ctx, world, o, "flame", 100, "particles_1x_flame");
        long[] two = burst(ctx, world, o, "flame", 200, "particles_2x_flame");
        long[] crit = burst(ctx, world, o, "crit", 0, "particles_0x_crit_kept");
        ctx.runOnClient(mc -> m().keepImportant.set(false));
        long[] critGone = burst(ctx, world, o, "crit", 0, "particles_0x_crit_not_kept");
        ctx.runOnClient(mc -> {
            m().keepImportant.set(true);
            m().ambientOff.set(true);
        });
        long[] ambient = burst(ctx, world, o, "flame", 100, "particles_no_ambient");
        ctx.runOnClient(mc -> m().ambientOff.set(false));
        check(none[0] == 0 && none[1] >= BURST, "0 %: no flames, created " + none[0]);
        check(one[0] >= BURST && one[0] <= BURST + BACKGROUND && one[2] == 0, "100 %: one each, created " + one[0]);
        check(two[2] >= BURST && two[0] >= 2 * BURST && two[0] <= 2 * BURST + 2 * BACKGROUND, "200 %: two each, created " + two[0]);
        check(crit[0] >= BURST && crit[0] <= BURST + BACKGROUND, "crits kept at 0 %, created " + crit[0]);
        check(critGone[0] == 0, "crits hidden when not kept, created " + critGone[0]);
        check(ambient[0] == 0, "no ambient hides flames, created " + ambient[0]);
    }

    private static long[] burst(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o, String type, int percent,
                                String shot) {
        ctx.runOnClient(mc -> {
            m().particles.set(percent);
            mc.particleEngine.clearParticles();
            ParticleGate.resetStats();
        });
        ctx.waitTicks(2);
        cmd(world, "particle minecraft:%s %.1f %.1f %.1f 1.2 0.6 0.4 0.02 %d force", type, o[0] + 0.5, o[1] + 1.2, o[2] - 4.0, BURST);
        ctx.waitTicks(3);
        long[] s = ctx.computeOnClient(mc -> ParticleGate.stats());
        String engine = ctx.computeOnClient(mc -> mc.particleEngine.countParticles());
        KIT.log("particles %s at %d %%: created %d, dropped %d, extra %d, engine %s", type, percent, s[0], s[1], s[2], engine);
        KIT.shotNow(ctx, shot);
        return s;
    }
}
