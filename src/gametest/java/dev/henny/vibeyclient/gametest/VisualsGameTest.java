package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.module.visuals.VisualsModule;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.screen.ModulesPanel;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import static dev.henny.vibeyclient.gametest.ReviewKit.check;
import static dev.henny.vibeyclient.gametest.ReviewKit.cmd;

public final class VisualsGameTest implements FabricClientGameTest {
    static final ReviewKit KIT = new ReviewKit("visuals");

    static VisualsModule m() {
        return ModuleManager.get().get(VisualsModule.class);
    }

    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        ctx.runOnClient(mc -> {
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
            GlobalSettings.language.set(GlobalSettings.Language.ENGLISH);
            check(m() != null && m().isEnabled(), "visuals module should be on");
            m().settings().all().forEach(s -> s.reset());
        });
        page(ctx, GlobalSettings.Language.ENGLISH, "page_en");
        page(ctx, GlobalSettings.Language.GERMAN, "page_de");
        try (TestSingleplayerContext world = ctx.worldBuilder().setUseConsistentSettings(false)
                .adjustSettings(s -> s.setSeed("vibeycamera")).create()) {
            world.getClientWorld().waitForChunksRender();
            int[] o = ReviewKit.platform(ctx, world, "smooth_stone");
            cmd(world, "gamerule send_command_feedback false");
            cmd(world, "gamerule advance_weather false");
            VisualsItemChecks.items(ctx, world, o);
            VisualsParticleChecks.particles(ctx, world, o);
            VisualsItemChecks.frameTime(ctx, world, o);
            ctx.runOnClient(mc -> m().settings().all().forEach(s -> s.reset()));
        }
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.AUTO));
    }

    private static void page(ClientGameTestContext ctx, GlobalSettings.Language lang, String name) {
        ctx.runOnClient(mc -> GlobalSettings.language.set(lang));
        ModulesPanel.preselect("visuals");
        ctx.setScreen(() -> new VibeyScreen(null));
        ctx.waitTicks(10);
        KIT.shot(ctx, name);
        ctx.setScreen(() -> null);
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.ENGLISH));
    }
}
