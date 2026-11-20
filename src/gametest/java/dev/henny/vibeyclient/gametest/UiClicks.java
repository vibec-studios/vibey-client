package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.ui.vui.Menus;
import dev.henny.vibeyclient.ui.vui.corner.Decorations;
import dev.henny.vibeyclient.ui.vui.menu.MenuScreen;
import net.minecraft.client.gui.screens.TitleScreen;

final class UiClicks {
    private final UiTest t;

    UiClicks(UiTest t) {
        this.t = t;
    }

    void titleToPage() {
        t.ctx().setScreen(TitleScreen::new);
        t.ctx().waitTicks(6);
        int[] v = t.ctx().computeOnClient(mc -> Decorations.center("vibey"));
        UiTest.check(v != null, "no Vibey corner button");
        t.click(v[0], v[1]);
        t.ctx().waitTicks(6);
        UiTest.check(t.ctx().computeOnClient(mc -> mc.screen instanceof MenuScreen),
                "clicking the Vibey corner button did not open the menu: " + t.ctx().computeOnClient(mc -> String.valueOf(mc.screen)));
        float[] tile = t.box("tile-emotes");
        UiTest.check(tile != null, "no emotes tile");
        t.click(tile[0] + 30, tile[1] + tile[3] / 2);
        t.ctx().waitTicks(6);
        String page = t.ctx().computeOnClient(mc -> pageId());
        UiTest.check("emotes".equals(page), "clicking the emotes tile opened " + page);
        t.shot("ui_click_module_page");
        t.ctx().getInput().pressKey(Keys.ESCAPE);
        t.ctx().waitTicks(4);
        UiTest.check(t.ctx().computeOnClient(mc -> pageId()) == null, "Esc did not go back to the grid");
        UiTest.check(t.ctx().computeOnClient(mc -> mc.screen instanceof MenuScreen), "Esc on a page closed the menu");
    }

    void tileSwitch() {
        t.ctx().runOnClient(mc -> Menus.open(null));
        t.ctx().waitTicks(6);
        boolean before = t.ctx().computeOnClient(mc -> ModuleManager.get().isEnabled("emotes"));
        float[] sw = t.box("#tile-emotes .tile-switch");
        UiTest.check(sw != null, "no switch on the emotes tile");
        t.click(sw[0] + sw[2] / 2, sw[1] + sw[3] / 2);
        t.ctx().waitTicks(4);
        boolean after = t.ctx().computeOnClient(mc -> ModuleManager.get().isEnabled("emotes"));
        UiTest.check(before != after, "the tile switch did not toggle the module");
        UiTest.check(t.ctx().computeOnClient(mc -> pageId()) == null, "the tile switch must not open the page");
        t.ctx().runOnClient(mc -> ModuleManager.get().setEnabled("emotes", before));
    }

    void gearAtScales() {
        for (int scale : new int[] {2, 3, 4}) {
            t.scale(scale);
            t.ctx().runOnClient(mc -> Menus.open(null));
            t.ctx().waitTicks(6);
            t.click("open-quick");
            t.ctx().waitTicks(4);
            String panel = t.ctx().computeOnClient(mc -> UiScreenShots.controller(mc).panel.peek());
            UiTest.check("quick".equals(panel), "gear click at GUI scale " + scale + " opened '" + panel + "'");
            t.click("close-menu");
            t.ctx().waitTicks(3);
            UiTest.check(t.ctx().computeOnClient(mc -> !(mc.screen instanceof MenuScreen)), "close click at scale " + scale + " did not close");
        }
    }

    void uiScaleOverride() {
        t.window(1920, 1080);
        t.scale(0);
        t.ctx().runOnClient(mc -> GlobalSettings.uiScale.set(GlobalSettings.UiScale.X2));
        t.ctx().runOnClient(mc -> Menus.open(null));
        t.ctx().waitTicks(6);
        int open = t.ctx().computeOnClient(mc -> (int) mc.getWindow().getGuiScale());
        UiTest.check(open == 2, "menu size 2x not applied: " + open);
        t.shot("ui_menu_1920_uiscale2");
        t.ctx().setScreen(() -> null);
        int restored = t.ctx().computeOnClient(mc -> (int) mc.getWindow().getGuiScale());
        UiTest.check(restored == 4, "GUI scale not restored after closing the menu: " + restored);
        t.ctx().runOnClient(mc -> GlobalSettings.uiScale.set(GlobalSettings.UiScale.GAME));
        t.window(1280, 720);
    }

    private static String pageId() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (!(mc.screen instanceof MenuScreen s)) return null;
        var p = s.controller().page.peek();
        return p == null ? null : p.getId();
    }
}
