package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class VibeyUiGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        UiTest t = new UiTest(ctx, "ui-new");
        UiScreenShots shots = new UiScreenShots(t);
        t.window(1280, 720);
        for (int scale : new int[] {3, 2}) {
            t.scale(scale);
            String size = scale == 3 ? "427" : "640";
            t.lang(GlobalSettings.Language.ENGLISH);
            shots.all(size + "_en", true);
            t.lang(GlobalSettings.Language.GERMAN);
            shots.all(size + "_de", true);
        }
        t.lang(GlobalSettings.Language.ENGLISH);
        t.scale(2);
        UiClicks clicks = new UiClicks(t);
        clicks.titleToPage();
        clicks.tileSwitch();
        clicks.gearAtScales();
        clicks.uiScaleOverride();
        new UiPause(t).run();
        ctx.setScreen(() -> null);
        t.lang(GlobalSettings.Language.AUTO);
        t.scale(0);
    }
}
