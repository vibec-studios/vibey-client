package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.service.Notify;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.ui.vui.Menus;
import dev.henny.vibeyclient.ui.vui.corner.Decorations;
import dev.henny.vibeyclient.ui.vui.dialog.Confirm;
import dev.henny.vibeyclient.ui.vui.menu.MenuController;
import dev.henny.vibeyclient.ui.vui.menu.MenuScreen;
import dev.henny.vibeyclient.ui.vui.onboarding.Onboarding;
import net.minecraft.client.gui.screens.TitleScreen;

final class UiScreenShots {
    private final UiTest t;

    UiScreenShots(UiTest t) {
        this.t = t;
    }

    void all(String tag, boolean extra) {
        title(tag);
        menu(tag, null, "", "menu_grid");
        menu(tag, "cosmetics", "", "menu_page");
        menu(tag, "media", "", "menu_media");
        more(tag);
        menu(tag, null, "quick", "menu_quick");
        menu(tag, null, "about", "menu_about");
        menu(tag, null, "friends", "menu_friends");
        account(tag);
        if (!extra) return;
        search(tag);
        onboarding(tag);
        toast(tag);
        confirm(tag);
    }

    private void title(String tag) {
        t.ctx().setScreen(TitleScreen::new);
        t.ctx().waitTicks(6);
        t.shot("ui_title_" + tag);
        int[] c = t.ctx().computeOnClient(mc -> Decorations.center("wardrobe"));
        UiTest.check(c != null, "no wardrobe corner button");
        t.hover(c[0], c[1]);
        t.ctx().waitTicks(14);
        t.shot("ui_title_hover_" + tag);
        t.away();
    }

    void menu(String tag, String module, String panel, String name) {
        t.ctx().runOnClient(mc -> Menus.openAt(null, module, panel));
        t.ctx().waitTicks(8);
        t.assertNoErrors();
        t.shot("ui_" + name + "_" + tag);
    }

    private void account(String tag) {
        boolean shown = t.ctx().computeOnClient(mc -> dev.henny.vibeyclient.api.Services.get(dev.henny.vibeyclient.api.service.FriendsDrawer.class)
                .map(d -> {
                    d.openAccount();
                    return true;
                }).orElse(false));
        if (!shown) return;
        t.ctx().waitTicks(8);
        t.assertNoErrors();
        t.shot("ui_account_" + tag);
    }

    private void more(String tag) {
        t.ctx().runOnClient(mc -> Menus.openModule(null, "multiplayer"));
        t.ctx().waitTicks(6);
        t.ctx().runOnClient(mc -> controller(mc).moreOpen.set(true));
        t.ctx().waitTicks(4);
        if (t.has("page-more")) {
            float[] c = t.center("page-more");
            t.hover(c[0], c[1] - 20);
            t.ctx().getInput().scroll(-10);
            t.ctx().waitTicks(8);
        }
        t.shot("ui_menu_more_" + tag);
        t.away();
    }

    private void search(String tag) {
        t.ctx().runOnClient(mc -> Menus.open(null));
        t.ctx().waitTicks(4);
        t.ctx().getInput().typeChars("show");
        t.ctx().waitTicks(6);
        String q = t.ctx().computeOnClient(mc -> controller(mc).query.peek());
        UiTest.check("show".equals(q), "typing anywhere should search, query=" + q);
        int hits = t.ctx().computeOnClient(mc -> controller(mc).modules.size());
        UiTest.check(hits > 0, "search 'show' found nothing");
        t.shot("ui_menu_search_" + tag);
        t.ctx().getInput().typeChars("zzqq");
        t.ctx().waitTicks(4);
        t.shot("ui_menu_search_empty_" + tag);
        t.ctx().getInput().pressKey(Keys.ESCAPE);
        t.ctx().getInput().pressKey(Keys.ESCAPE);
        t.ctx().waitTicks(2);
        String cleared = t.ctx().computeOnClient(mc -> mc.screen instanceof MenuScreen s ? s.controller().query.peek() : "closed");
        UiTest.check(cleared.isEmpty() || cleared.equals("closed"), "Esc should clear the search, query=" + cleared);
    }

    private void onboarding(String tag) {
        t.ctx().setScreen(() -> Onboarding.create(new TitleScreen()));
        t.ctx().waitTicks(6);
        t.shot("ui_onboarding1_" + tag);
        for (int step = 2; step <= 3; step++) {
            t.ctx().getInput().pressKey(Keys.RIGHT);
            t.ctx().waitTicks(6);
            int now = t.ctx().computeOnClient(mc -> ((Onboarding) mc.screen).controller().step.peek());
            UiTest.check(now == step - 1, "Right arrow should go to step " + step + ", is " + (now + 1));
            t.shot("ui_onboarding" + step + "_" + tag);
        }
    }

    private void toast(String tag) {
        t.ctx().runOnClient(mc -> {
            Menus.open(null);
            Notify.show(new Notify.Toast("PolarPia", "Online", Notify.Kind.SOCIAL, null, null));
            Notify.show(new Notify.Toast("New cosmetics", "3 items", Notify.Kind.INFO, null, () -> {}));
        });
        t.ctx().waitTicks(12);
        t.shot("ui_toast_" + tag);
    }

    private void confirm(String tag) {
        Confirm[] c = new Confirm[1];
        t.ctx().runOnClient(mc -> c[0] = Confirm.ask("person", "Sign out?", "You stay signed out.", "Sign out", true, () -> {}));
        t.ctx().waitTicks(8);
        t.shot("ui_confirm_" + tag);
        t.ctx().getInput().pressKey(Keys.ESCAPE);
        t.ctx().waitTicks(3);
        UiTest.check(!t.ctx().computeOnClient(mc -> c[0].isOpen()), "Esc should cancel the confirm dialog");
        UiTest.check(t.ctx().computeOnClient(mc -> mc.screen instanceof MenuScreen), "Esc on the dialog must not close the menu");
    }

    static MenuController controller(net.minecraft.client.Minecraft mc) {
        if (!(mc.screen instanceof MenuScreen s)) throw new AssertionError("menu not open: " + mc.screen);
        return s.controller();
    }
}
