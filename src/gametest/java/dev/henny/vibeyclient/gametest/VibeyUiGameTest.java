package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.ui.VibeyScreen;
import dev.henny.vibeyclient.ui.title.OnboardingScreen;
import dev.henny.vibeyclient.ui.title.VibeyTitleScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.PauseScreen;

/**
 * Screenshots of the client UI (title screen, onboarding, Vibey menu, pause menu) at GUI scales 2, 3 and auto and
 * window sizes 1280×720 / 1920×1080, in English and German. Files: versions/fabric-1.21.11/build/run/clientGameTest/
 * screenshots/ui_*.png. Run: ./gradlew :fabric-1.21.11:runClientGameTest
 */
public final class VibeyUiGameTest implements FabricClientGameTest {
    private static final int[][] SIZES = {{1280, 720}, {1920, 1080}};
    private static final int[] SCALES = {2, 3, 0};

    @Override
    public void runTest(ClientGameTestContext ctx) {
        for (int[] size : SIZES) {
            ctx.getInput().resizeWindow(size[0], size[1]);
            for (int scale : SCALES) {
                setScale(ctx, scale);
                String tag = size[0] + "_s" + (scale == 0 ? "auto" : scale);
                boolean full = size[0] == 1280 && scale != 0;
                lang(ctx, GlobalSettings.Language.ENGLISH);
                titleScreens(ctx, tag, full);
                if (full) {
                    lang(ctx, GlobalSettings.Language.GERMAN);
                    titleScreens(ctx, tag + "_de", true);
                    lang(ctx, GlobalSettings.Language.ENGLISH);
                }
            }
        }

        // in a world: pause menu with the Vibey row, at the small and the big scales
        ctx.getInput().resizeWindow(1280, 720);
        try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            for (int scale : new int[] {2, 3, 4}) {
                setScale(ctx, scale);
                ctx.setScreen(() -> new PauseScreen(true));
                ctx.waitTicks(4);
                ctx.takeScreenshot("ui_pause_1280_s" + scale);
            }
            setScale(ctx, 3);
            lang(ctx, GlobalSettings.Language.GERMAN);
            ctx.setScreen(() -> new PauseScreen(true));
            ctx.waitTicks(4);
            ctx.takeScreenshot("ui_pause_1280_s3_de");
            lang(ctx, GlobalSettings.Language.ENGLISH);
            ctx.setScreen(() -> null);
        }
        lang(ctx, GlobalSettings.Language.AUTO);
        setScale(ctx, 0);
    }

    private static void titleScreens(ClientGameTestContext ctx, String tag, boolean full) {
        ctx.setScreen(() -> new VibeyTitleScreen(false));
        ctx.waitTicks(6);
        ctx.takeScreenshot("ui_title_" + tag);
        if (!full) return;
        // keyboard focus on the second entry
        ctx.getInput().pressKey(Keys.DOWN);
        ctx.getInput().pressKey(Keys.DOWN);
        ctx.waitTicks(4);
        ctx.takeScreenshot("ui_title_focus_" + tag);

        ctx.setScreen(() -> new OnboardingScreen(new VibeyTitleScreen(false)));
        ctx.waitTicks(6);
        ctx.takeScreenshot("ui_onboarding1_" + tag);
        ctx.getInput().pressKey(Keys.RIGHT);
        ctx.waitTicks(6);
        ctx.takeScreenshot("ui_onboarding2_" + tag);
        ctx.getInput().pressKey(Keys.RIGHT);
        ctx.waitTicks(6);
        ctx.takeScreenshot("ui_onboarding3_" + tag);

        ctx.setScreen(() -> new VibeyScreen(null));
        ctx.waitTicks(6);
        ctx.takeScreenshot("ui_menu_" + tag);
    }

    private static void setScale(ClientGameTestContext ctx, int scale) {
        ctx.runOnClient(mc -> {
            mc.options.guiScale().set(scale);
            mc.resizeDisplay();
        });
        ctx.waitTicks(2);
    }

    private static void lang(ClientGameTestContext ctx, GlobalSettings.Language l) {
        ctx.runOnClient(mc -> {
            GlobalSettings.language.set(l);
            dev.henny.vibeyclient.api.Lang.tick();
        });
    }
}
