package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.module.visuals.particles.ParticleGate;
import dev.henny.vibeyclient.module.visuals.particles.StyleStore;
import dev.henny.vibeyclient.module.visuals.particles.core.ParticleStyle;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.particles.ParticleTypes;

import static dev.henny.vibeyclient.gametest.EffectsGameTest.KIT;
import static dev.henny.vibeyclient.gametest.ReviewKit.check;
import static dev.henny.vibeyclient.gametest.ReviewKit.cmd;

final class EffectsParticleChecks {
    static final int BURST = 120;
    private static final int BACKGROUND = 40;

    private EffectsParticleChecks() {}

    static void particles(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        ReviewKit.look(ctx, 180, 8);
        long vanilla = burst(ctx, world, o, "end_rod", ParticleStyle.VANILLA, "particles_end_rod_vanilla");
        long triple = burst(ctx, world, o, "end_rod", ParticleStyle.VANILLA.withAmount(300), "particles_end_rod_300");
        long hidden = burst(ctx, world, o, "end_rod", ParticleStyle.VANILLA.withHidden(true), "particles_end_rod_hidden");
        long hiddenDropped = ctx.computeOnClient(mc -> ParticleGate.stats()[1]);
        burst(ctx, world, o, "end_rod", new ParticleStyle(100, 260, 0xFF3B3B, 100, false), "particles_end_rod_big_red");
        burst(ctx, world, o, "flame", new ParticleStyle(100, 200, 0x4D7BFF, 80, false), "particles_flame_big_blue");
        long crit = burst(ctx, world, o, "crit", ParticleStyle.VANILLA.withSize(220).withTint(0xFFE14D).withStrength(100),
                "particles_crit_big_yellow");
        check(vanilla >= BURST && vanilla <= BURST + BACKGROUND, "vanilla end rods: " + vanilla);
        check(triple >= 3 * BURST && triple <= 3 * BURST + 3 * BACKGROUND, "300 %: three each, created " + triple);
        check(hidden <= BURST / 10 && hiddenDropped >= BURST, "hidden end rods: " + hidden + ", dropped " + hiddenDropped);
        check(crit >= BURST, "styled crits still spawn: " + crit);
        int tinted = ctx.computeOnClient(mc -> StyleStore.slot(ParticleTypes.CRIT).color(0xFFFFFFFF));
        check(tinted == 0xFFFFE14D, String.format("crit tint colour %08X", tinted));
        float size = ctx.computeOnClient(mc -> StyleStore.slot(ParticleTypes.CRIT).size(0.1f));
        check(Math.abs(size - 0.22f) < 1e-4, "crit size factor " + size);
        ctx.runOnClient(mc -> EffectsGameTest.resetAll());
    }

    static long burst(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o, String type, ParticleStyle style,
                      String shot) {
        ctx.waitTicks(8);
        ctx.runOnClient(mc -> {
            StyleStore.set("minecraft:" + type, style);
            mc.particleEngine.clearParticles();
            ParticleGate.resetStats();
        });
        ctx.waitTicks(2);
        cmd(world, "particle minecraft:%s %.1f %.1f %.1f 0.8 0.5 0.3 0.01 %d force", type, o[0] - 0.5, o[1] + 1.6, o[2] - 3.5, BURST);
        ctx.waitTicks(3);
        long[] s = ctx.computeOnClient(mc -> ParticleGate.stats());
        KIT.log("particles %s %s: created %d, dropped %d, extra %d", type, style, s[0], s[1], s[2]);
        KIT.shotNow(ctx, shot);
        return s[0];
    }
}
