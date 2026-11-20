package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.module.camera.CameraModule;
import dev.henny.vibeyclient.module.camera.CameraRules;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.screen.ModulesPanel;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import static dev.henny.vibeyclient.gametest.ReviewKit.check;

public final class CameraGameTest implements FabricClientGameTest {
    static final ReviewKit KIT = new ReviewKit("camera");

    static CameraModule m() {
        return ModuleManager.get().get(CameraModule.class);
    }

    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        ctx.runOnClient(mc -> {
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
            GlobalSettings.language.set(GlobalSettings.Language.ENGLISH);
            check(m() != null && m().isEnabled(), "camera module should be on");
            for (String key : CameraRules.ALL) check(ServerRules.supportedKeys().contains(key), "rule key not declared: " + key);
            check(!m().freelook.on() && !m().motionBlur.on(), "freelook and motion blur must be off by default");
            m().settings().all().forEach(s -> s.reset());
        });
        page(ctx, GlobalSettings.Language.ENGLISH, "page_en");
        page(ctx, GlobalSettings.Language.GERMAN, "page_de");
        try (TestSingleplayerContext world = ctx.worldBuilder().setUseConsistentSettings(false)
                .adjustSettings(s -> s.setSeed("vibeycamera")).create()) {
            world.getClientWorld().waitForChunksRender();
            int[] o = CameraScene.build(ctx, world);
            CameraLookChecks.freelook(ctx, o);
            CameraLookChecks.rules(ctx);
            CameraLookChecks.zoomTogether(ctx);
            CameraOrbitChecks.orbit(ctx, world, o);
            CameraBlurChecks.blur(ctx, o);
            ctx.runOnClient(mc -> m().settings().all().forEach(s -> s.reset()));
        }
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.AUTO));
    }

    private static void page(ClientGameTestContext ctx, GlobalSettings.Language lang, String name) {
        ctx.runOnClient(mc -> GlobalSettings.language.set(lang));
        ModulesPanel.preselect("camera");
        ctx.setScreen(() -> new VibeyScreen(null));
        ctx.waitTicks(10);
        KIT.shot(ctx, name);
        ctx.setScreen(() -> null);
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.ENGLISH));
    }
}
