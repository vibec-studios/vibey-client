package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.module.visuals.VisualsModule;
import dev.henny.vibeyclient.ui.vui.Menus;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import static dev.henny.vibeyclient.gametest.ReviewKit.check;
import static dev.henny.vibeyclient.gametest.ReviewKit.cmd;

public final class ItemModelsGameTest implements FabricClientGameTest {
    static final ReviewKit KIT = new ReviewKit("items");

    static VisualsModule m() {
        return ModuleManager.get().get(VisualsModule.class);
    }

    static void reset(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> m().settings().all().forEach(s -> s.reset()));
        ctx.waitTicks(2);
    }

    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        ctx.runOnClient(mc -> {
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
            GlobalSettings.language.set(GlobalSettings.Language.ENGLISH);
            check(m() != null && m().isEnabled(), "visuals module should be on");
        });
        reset(ctx);
        try (TestSingleplayerContext world = ctx.worldBuilder().setUseConsistentSettings(false)
                .adjustSettings(s -> s.setSeed("vibeycamera")).create()) {
            world.getClientWorld().waitForChunksRender();
            int[] o = ReviewKit.platform(ctx, world, "smooth_stone");
            cmd(world, "gamerule send_command_feedback false");
            cmd(world, "gamerule advance_weather false");
            page(ctx, GlobalSettings.Language.ENGLISH, "page_en");
            page(ctx, GlobalSettings.Language.GERMAN, "page_de");
            ItemHeldChecks.poses(ctx, world);
            ItemEditorChecks.editor(ctx, world);
            ItemHeldChecks.swing(ctx);
            ItemWorldChecks.glint(ctx, world);
            ItemWorldChecks.drops(ctx, world, o);
            ItemWorldChecks.frames(ctx, world, o);
            reset(ctx);
        }
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.AUTO));
    }

    private static void page(ClientGameTestContext ctx, GlobalSettings.Language lang, String name) {
        ctx.runOnClient(mc -> GlobalSettings.language.set(lang));
        ctx.waitTicks(10);
        ctx.runOnClient(mc -> Menus.openModule(null, "visuals"));
        ctx.waitTicks(10);
        KIT.shot(ctx, name);
        ctx.setScreen(() -> null);
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.ENGLISH));
        ctx.waitTicks(2);
    }
}
