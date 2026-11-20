package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.GlobalSettings.UiTheme;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.hud.editor.HudEditorScreen;
import dev.henny.vibeyclient.ui.vui.Menus;
import dev.henny.vibeyclient.ui.vui.theme.ThemeAudit;
import dev.henny.vibeyclient.ui.vui.theme.UiThemes;

import java.util.Locale;
import java.util.Map;

final class PolishThemes {
    private static final String PAGE = "waypoints";

    private final UiTest t;

    PolishThemes(UiTest t) {
        this.t = t;
    }

    void allThemes(GlobalSettings.Language lang, String tag) {
        t.lang(lang);
        for (UiTheme theme : UiTheme.values()) {
            apply(theme);
            String name = "polish_" + tag + "_" + theme.name().toLowerCase(Locale.ROOT);
            t.ctx().setScreen(() -> null);
            t.ctx().runOnClient(mc -> Menus.open(null));
            t.ctx().waitTicks(8);
            t.assertNoErrors();
            t.shot(name + "_1_grid");
            t.ctx().runOnClient(mc -> Menus.openModule(null, PAGE));
            t.ctx().waitTicks(8);
            t.shot(name + "_2_page");
            t.ctx().runOnClient(mc -> HudLayout.setFlag("intro", true));
            t.ctx().setScreen(() -> new HudEditorScreen(null));
            t.ctx().waitTicks(10);
            t.assertNoErrors();
            t.shot(name + "_3_hud");
            t.ctx().setScreen(() -> null);
        }
    }

    void customAccent() {
        apply(UiTheme.LIGHT);
        t.ctx().runOnClient(mc -> {
            GlobalSettings.accent.set(0xFF13A387);
            GlobalSettings.ownAccent.set(true);
        });
        t.ctx().waitTicks(3);
        Map<String, String> tokens = t.ctx().computeOnClient(mc -> Map.copyOf(UiThemes.active()));
        UiTest.check(!"#6646e6".equals(tokens.get("brand")) && tokens.get("brand").startsWith("#0") || tokens.get("brand").startsWith("#1"),
                "own accent not applied: " + tokens.get("brand"));
        System.out.println("[vibeyclient-test] accent #13a387 on light -> " + tokens.get("brand") + " / " + tokens.get("on-brand"));
        UiTest.check(ThemeAudit.failures(tokens).isEmpty(), "accent contrast: " + ThemeAudit.failures(tokens));
        t.ctx().runOnClient(mc -> Menus.openQuick(null));
        t.ctx().waitTicks(8);
        t.shot("polish_en_accent_quick");
        t.ctx().runOnClient(mc -> GlobalSettings.ownAccent.set(false));
    }

    void reset() {
        apply(UiTheme.DARK);
    }

    private void apply(UiTheme theme) {
        t.ctx().runOnClient(mc -> GlobalSettings.theme.set(theme));
        t.ctx().waitTicks(3);
        Map<String, String> tokens = t.ctx().computeOnClient(mc -> Map.copyOf(UiThemes.active()));
        UiTest.check(ThemeAudit.failures(tokens).isEmpty(), theme + " contrast: " + ThemeAudit.failures(tokens));
        String want = theme == UiTheme.LIGHT ? "#6646e6" : theme == UiTheme.CONTRAST ? "#b9a8ff" : null;
        if (want != null) UiTest.check(want.equals(tokens.get("brand")), theme + " not applied, brand " + tokens.get("brand"));
        System.out.println("[vibeyclient-test] theme " + theme + " " + ThemeAudit.contrast(tokens));
    }
}
