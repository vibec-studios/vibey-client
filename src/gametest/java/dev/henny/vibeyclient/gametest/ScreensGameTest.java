package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.module.screens.Screens;
import dev.henny.vibeyclient.module.screens.edit.EditMode;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.screen.ModulesPanel;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import static dev.henny.vibeyclient.gametest.ScreensTestKit.*;

public final class ScreensGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        ctx.runOnClient(mc -> {
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
            GlobalSettings.language.set(GlobalSettings.Language.ENGLISH);
            check(m() != null && m().isEnabled(), "screens module should be on");
            m().settings().all().forEach(s -> s.reset());
            Screens.store().clear();
            EditMode.drawPointer = true;
        });
        ctx.waitTicks(5);
        ScreensOptionsChecks.run(ctx);
        ScreensTitleChecks.run(ctx);
        try (TestSingleplayerContext world = ctx.worldBuilder().setUseConsistentSettings(false)
                .adjustSettings(s -> s.setSeed("vibeyscreens")).create()) {
            world.getClientWorld().waitForChunksRender();
            int[] o = ReviewKit.platform(ctx, world, "stone");
            ReviewKit.cmd(world, "gamemode survival @a");
            ctx.waitTicks(5);
            ScreensContainerChecks.inventory(ctx, world);
            ScreensContainerChecks.chest(ctx, world, o);
            check(!ctx.computeOnClient(mc -> Screens.store().names().isEmpty()), "container layouts were not stored");
        }
        page(ctx, GlobalSettings.Language.ENGLISH, "page_en");
        page(ctx, GlobalSettings.Language.GERMAN, "page_de");
        ctx.runOnClient(mc -> {
            m().resetAll.run();
            check(Screens.store().names().isEmpty(), "Reset all left layouts");
            EditMode.drawPointer = false;
            GlobalSettings.language.set(GlobalSettings.Language.AUTO);
        });
    }

    private static void page(ClientGameTestContext ctx, GlobalSettings.Language lang, String name) {
        ctx.runOnClient(mc -> GlobalSettings.language.set(lang));
        ctx.waitTicks(3);
        ModulesPanel.preselect("screens");
        ctx.setScreen(() -> new VibeyScreen(null));
        ctx.waitTicks(10);
        shot(ctx, name);
        ctx.setScreen(() -> null);
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.ENGLISH));
    }
}
