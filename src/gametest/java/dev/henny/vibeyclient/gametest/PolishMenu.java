package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.ui.vui.Menus;
import dev.henny.vibeyclient.ui.vui.menu.MenuController;
import dev.henny.vibeyclient.ui.vui.menu.MenuScreen;
import net.minecraft.client.Minecraft;

final class PolishMenu {
    private final UiTest t;

    PolishMenu(UiTest t) {
        this.t = t;
    }

    void searchHitOpensQuickSettings() {
        t.ctx().setScreen(() -> null);
        t.ctx().runOnClient(mc -> Menus.open(null));
        t.ctx().waitTicks(6);
        t.ctx().runOnClient(mc -> controller(mc).query.set("theme"));
        t.ctx().waitTicks(4);
        UiTest.check(t.has("qhit-theme"), "searching 'theme' shows no settings hit");
        t.shot("polish_en_search_theme");
        t.click("qhit-theme");
        t.ctx().waitTicks(6);
        String panel = t.ctx().computeOnClient(mc -> controller(mc).panel.peek());
        String focus = t.ctx().computeOnClient(mc -> controller(mc).quickFocus.peek());
        UiTest.check("quick".equals(panel) && focus.endsWith(".theme"), "hit should open quick settings at theme: " + panel + "/" + focus);
        t.shot("polish_en_search_theme_open");
        t.ctx().runOnClient(mc -> controller(mc).query.set("server"));
        t.ctx().waitTicks(4);
        UiTest.check(t.has("qhit-backendUrl"), "searching 'server' shows no hit for the Vibe server row");
        t.click("qhit-backendUrl");
        t.ctx().waitTicks(6);
        boolean more = t.ctx().computeOnClient(mc -> controller(mc).quickMoreOpen.peek());
        UiTest.check(more, "a hit in More should open More");
        t.shot("polish_en_search_server_open");
        t.ctx().setScreen(() -> null);
    }

    void devlinkStatusCard() {
        t.ctx().runOnClient(mc -> Menus.openModule(null, "devlink"));
        t.ctx().waitTicks(10);
        UiTest.check(t.has("page-status"), "devlink page has no status card");
        t.shot("polish_en_devlink_status");
        t.ctx().setScreen(() -> null);
    }

    private static MenuController controller(Minecraft mc) {
        if (!(mc.screen instanceof MenuScreen s)) throw new AssertionError("menu not open: " + mc.screen);
        return s.controller();
    }
}
