package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.module.pvp.PvpModule;
import dev.henny.vibeyclient.module.visuals.VisualsModule;
import dev.henny.vibeyclient.module.visuals.effects.EffectsPart;
import dev.henny.vibeyclient.module.visuals.effects.EffectsPreview;
import dev.henny.vibeyclient.module.visuals.particles.StyleStore;
import dev.henny.vibeyclient.ui.vui.Menus;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import java.util.Map;

import static dev.henny.vibeyclient.gametest.ReviewKit.check;
import static dev.henny.vibeyclient.gametest.ReviewKit.cmd;

public final class EffectsGameTest implements FabricClientGameTest {
    static final ReviewKit KIT = new ReviewKit("effects");

    static VisualsModule visuals() {
        return ModuleManager.get().get(VisualsModule.class);
    }

    static EffectsPart fx() {
        return EffectsPart.get();
    }

    static PvpModule pvp() {
        return ModuleManager.get().get(PvpModule.class);
    }

    static void resetAll() {
        visuals().settings().all().forEach(s -> s.reset());
        if (pvp() != null) pvp().settings().all().forEach(s -> s.reset());
        StyleStore.replaceAll(Map.of());
        EffectsPreview.clear();
        ServerRules.clear();
    }

    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        ctx.runOnClient(mc -> {
            mc.options.guiScale().set(3);
            mc.resizeDisplay();
            GlobalSettings.language.set(GlobalSettings.Language.ENGLISH);
            check(visuals() != null && visuals().isEnabled(), "visuals module should be on");
            check(fx() != null, "effects part missing");
            ModuleManager.get().setEnabled("pvp", true);
            resetAll();
        });
        page(ctx);
        try (TestSingleplayerContext world = ctx.worldBuilder().setUseConsistentSettings(false)
                .adjustSettings(s -> s.setSeed("vibeycamera")).create()) {
            world.getClientWorld().waitForChunksRender();
            int[] o = ReviewKit.platform(ctx, world, "smooth_stone");
            cmd(world, "gamerule send_command_feedback false");
            cmd(world, "gamerule advance_weather false");
            cmd(world, "setblock %d %d %d minecraft:oak_planks", o[0], o[1], o[2] - 3);
            EffectsEditorChecks.editor(ctx, world, o);
            EffectsOverlayChecks.overlays(ctx, world, o);
            EffectsWorldChecks.world(ctx, world, o);
            EffectsParticleChecks.particles(ctx, world, o);
            EffectsRuleChecks.rules(ctx, world, o);
            ctx.runOnClient(mc -> resetAll());
        }
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.AUTO));
    }

    private static void page(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> Menus.openModule(null, "visuals"));
        ctx.waitTicks(12);
        KIT.shot(ctx, "settings_page_en");
        ctx.setScreen(() -> null);
    }
}
