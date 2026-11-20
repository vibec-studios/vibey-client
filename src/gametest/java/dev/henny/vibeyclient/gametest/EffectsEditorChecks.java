package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.module.visuals.effects.screen.EffectsScreen;
import dev.henny.vibeyclient.module.visuals.particles.ParticleGate;
import dev.henny.vibeyclient.module.visuals.particles.StyleStore;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import static dev.henny.vibeyclient.gametest.EffectsGameTest.KIT;
import static dev.henny.vibeyclient.gametest.ReviewKit.check;

final class EffectsEditorChecks {
    private EffectsEditorChecks() {}

    static void editor(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        UiTest ui = new UiTest(ctx, "effects");
        ReviewKit.look(ctx, 180, 10);
        open(ctx);
        ui.assertNoErrors();
        check(ctx.computeOnClient(mc -> !mc.isPaused()), "the editor must not pause the game");
        KIT.shot(ctx, "editor_effects_en");
        float[] fire = ui.box("#test-fire-visuals-fireOpacity");
        check(fire != null, "no fire test button");
        ui.click(fire[0] + fire[2] / 2, fire[1] + fire[3] / 2);
        ctx.waitTicks(6);
        check(ctx.computeOnClient(mc -> dev.henny.vibeyclient.module.visuals.effects.EffectsPreview.fireOn()), "fire test did not start");
        KIT.shotNow(ctx, "editor_fire_test");
        ctx.runOnClient(mc -> {
            GlobalSettings.language.set(GlobalSettings.Language.GERMAN);
            dev.henny.vibeyclient.api.Lang.tick();
        });
        reopen(ctx);
        KIT.shot(ctx, "editor_effects_de");
        ctx.runOnClient(mc -> {
            GlobalSettings.language.set(GlobalSettings.Language.ENGLISH);
            dev.henny.vibeyclient.api.Lang.tick();
        });
        reopen(ctx);
        ctx.runOnClient(mc -> controller(mc).tab.set("particles"));
        ctx.waitTicks(8);
        ui.assertNoErrors();
        KIT.shot(ctx, "editor_particles_list");
        ctx.runOnClient(mc -> controller(mc).query.set("flame"));
        ctx.waitTicks(6);
        float[] row = ui.box("#p-minecraft-flame");
        check(row != null, "no flame row after searching");
        ctx.runOnClient(mc -> ParticleGate.resetStats());
        ui.click(row[0] + 40, row[1] + row[3] / 2);
        ctx.waitTicks(4);
        check(ctx.computeOnClient(mc -> controller(mc).sel.peek() != null), "clicking a row did not open the detail");
        long burst = ctx.computeOnClient(mc -> ParticleGate.stats()[0]);
        check(burst >= 20, "selecting flame should spawn a preview burst, created " + burst);
        ctx.runOnClient(mc -> {
            var d = controller(mc).sel.peek();
            d.amount.set(300);
            d.size.set(220);
            d.tint.set("#3fe0c5");
        });
        ctx.waitTicks(10);
        check(ctx.computeOnClient(mc -> StyleStore.styles().get("minecraft:flame").amount()) == 300, "amount slider not stored");
        KIT.shotNow(ctx, "editor_particle_detail");
        ctx.runOnClient(mc -> controller(mc).preset.set("fancy"));
        ctx.waitTicks(6);
        check(ctx.computeOnClient(mc -> StyleStore.styles().get("minecraft:crit").amount()) == 200, "Fancy preset did not apply");
        check("fancy".equals(ctx.computeOnClient(mc -> controller(mc).preset.peek())), "preset bar does not show Fancy");
        ctx.runOnClient(mc -> controller(mc).back());
        ctx.runOnClient(mc -> controller(mc).filter.set("changed"));
        ctx.runOnClient(mc -> controller(mc).query.set(""));
        ctx.waitTicks(8);
        KIT.shot(ctx, "editor_particles_fancy_changed");
        ctx.runOnClient(mc -> controller(mc).preset.set("vanilla"));
        ctx.waitTicks(4);
        check(ctx.computeOnClient(mc -> StyleStore.styles().changed()) == 0, "Vanilla preset should clear particle styles");
        ctx.setScreen(() -> null);
        ctx.runOnClient(mc -> EffectsGameTest.resetAll());
    }

    static void open(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> EffectsScreen.open(null));
        ctx.waitTicks(12);
        check(ctx.computeOnClient(mc -> mc.screen instanceof EffectsScreen), "editor did not open");
    }

    private static void reopen(ClientGameTestContext ctx) {
        ctx.setScreen(() -> null);
        open(ctx);
    }

    static dev.henny.vibeyclient.module.visuals.effects.screen.EffectsController controller(net.minecraft.client.Minecraft mc) {
        return ((EffectsScreen) mc.screen).controller();
    }
}
