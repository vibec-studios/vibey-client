package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.ui.vui.corner.Decorations;
import dev.henny.vibeyclient.ui.vui.menu.MenuScreen;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.PauseScreen;

final class UiPause {
    private final UiTest t;

    UiPause(UiTest t) {
        this.t = t;
    }

    void run() {
        t.window(1280, 720);
        try (TestSingleplayerContext world = t.ctx().worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            for (int scale : new int[] {2, 3, 4}) {
                t.scale(scale);
                pause("ui_pause_1280_s" + scale);
            }
            t.window(960, 540);
            t.scale(3);
            pause("ui_pause_960_s3");
            t.window(1280, 720);
            t.scale(3);
            t.lang(GlobalSettings.Language.GERMAN);
            pause("ui_pause_1280_s3_de");
            t.lang(GlobalSettings.Language.ENGLISH);
            clickVibey();
            t.ctx().setScreen(() -> null);
        }
    }

    private void pause(String name) {
        t.ctx().setScreen(() -> new PauseScreen(true));
        t.ctx().waitTicks(4);
        int[] c = t.ctx().computeOnClient(mc -> Decorations.center("friends"));
        if (c != null) {
            t.hover(c[0], c[1]);
            t.ctx().waitTicks(14);
        }
        t.shot(name);
        t.away();
    }

    private void clickVibey() {
        t.ctx().setScreen(() -> new PauseScreen(true));
        t.ctx().waitTicks(4);
        int[] c = t.ctx().computeOnClient(mc -> Decorations.center("vibey"));
        UiTest.check(c != null, "no Vibey button in the pause row");
        t.click(c[0], c[1]);
        t.ctx().waitTicks(5);
        UiTest.check(t.ctx().computeOnClient(mc -> mc.screen instanceof MenuScreen), "pause Vibey click did not open the menu");
        boolean pauses = t.ctx().computeOnClient(mc -> mc.screen.isPauseScreen());
        UiTest.check(pauses, "menu over the pause screen should keep the game paused");
    }
}
