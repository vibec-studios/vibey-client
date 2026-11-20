package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

public final class PolishGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        UiTest t = new UiTest(ctx, "polish");
        boolean online = ctx.computeOnClient(mc -> GlobalSettings.onlineFeatures.on());
        ctx.runOnClient(mc -> GlobalSettings.onlineFeatures.set(false));
        t.window(1280, 720);
        t.scale(3);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getServer().runCommand("time set 1000");
            world.getClientWorld().waitForChunksRender();
            PolishThemes themes = new PolishThemes(t);
            themes.allThemes(GlobalSettings.Language.ENGLISH, "en");
            themes.allThemes(GlobalSettings.Language.GERMAN, "de");
            t.lang(GlobalSettings.Language.ENGLISH);
            themes.customAccent();
            new PolishMenu(t).searchHitOpensQuickSettings();
            new PolishMenu(t).devlinkStatusCard();
            themes.reset();
            ctx.setScreen(() -> null);
        }
        t.lang(GlobalSettings.Language.AUTO);
        ctx.runOnClient(mc -> GlobalSettings.onlineFeatures.set(online));
        t.scale(0);
    }
}
