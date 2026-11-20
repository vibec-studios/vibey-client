package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.module.visuals.effects.EffectRules;
import dev.henny.vibeyclient.module.visuals.effects.Effects;
import dev.henny.vibeyclient.module.visuals.particles.core.ParticleStyle;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import java.util.List;

import static dev.henny.vibeyclient.gametest.EffectsGameTest.KIT;
import static dev.henny.vibeyclient.gametest.EffectsGameTest.fx;
import static dev.henny.vibeyclient.gametest.ReviewKit.check;

final class EffectsRuleChecks {
    private EffectsRuleChecks() {}

    static void rules(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        check(ctx.computeOnClient(mc -> ServerRules.supportedKeys().containsAll(EffectRules.ALL)), "rule keys not announced");
        ctx.runOnClient(mc -> {
            fx().fireOpacity.set(20);
            fx().totemSize.set(50);
            fx().pumpkinOpacity.set(10);
            fx().ownPotionParticles.set(false);
            ServerRules.apply(List.of("visuals.*"), "No reduced effects here.");
        });
        ctx.waitTicks(2);
        check(Math.abs(ctx.computeOnClient(mc -> Effects.fireAlpha(0.9f)) - 0.9f) < 1e-4, "fire must be vanilla while blocked");
        check(ctx.computeOnClient(mc -> Effects.totemScale()) == 1f, "totem must be vanilla while blocked");
        check(ctx.computeOnClient(mc -> Effects.overlayAlpha(1f)) == 1f, "pumpkin must be vanilla while blocked");
        check(!ctx.computeOnClient(mc -> Effects.hideOwnPotionParticles()), "own particles must show while blocked");
        long blocked = EffectsParticleChecks.burst(ctx, world, o, "end_rod", ParticleStyle.VANILLA.withHidden(true),
                "rules_hidden_particles_shown");
        check(blocked >= EffectsParticleChecks.BURST, "hidden particles must come back while blocked: " + blocked);
        EffectsEditorChecks.open(ctx);
        KIT.shot(ctx, "rules_editor_badges");
        ctx.setScreen(() -> null);
        ctx.runOnClient(mc -> ServerRules.clear());
        ctx.waitTicks(2);
        check(Math.abs(ctx.computeOnClient(mc -> Effects.fireAlpha(0.9f)) - 0.18f) < 1e-4, "fire setting back after leaving");
        ctx.runOnClient(mc -> EffectsGameTest.resetAll());
    }
}
