package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.hud.HudShare;
import dev.henny.vibeyclient.ui.hud.HudStyle;
import dev.henny.vibeyclient.ui.hud.editor.HudEditorScreen;

import static dev.henny.vibeyclient.gametest.HudTestKit.check;

final class HudEditorPanelsCheck {
    private HudEditorPanelsCheck() {}

    static void run(HudTestKit t) {
        contextMenu(t);
        stylePanel(t);
        library(t);
        presets(t);
        shareImport(t);
        duplicate(t);
    }

    private static String panel(HudTestKit t) {
        return t.context.computeOnClient(mc -> ((HudEditorScreen) mc.screen).controller().panel.peek());
    }

    private static void contextMenu(HudTestKit t) {
        HudLayout.Box e = t.box("hud.effects");
        t.point(e.x + e.w / 2, e.y + e.h / 2);
        t.context.getInput().pressMouse(1);
        t.context.waitTicks(5);
        check("menu".equals(panel(t)), "right-click did not open the menu: " + panel(t));
        t.shot("hud_09_context_menu");
        t.click("menu-style");
        t.context.waitTicks(6);
        check("style".equals(panel(t)), "menu → style: " + panel(t));
        t.point(5, 120);
        t.shot("hud_10_style_box");
    }

    private static void stylePanel(HudTestKit t) {
        t.context.runOnClient(mc -> ((HudEditorScreen) mc.screen).controller().styleTab("text"));
        t.context.waitTicks(4);
        t.shot("hud_10_style_text");
        float[] rainbow = t.context.computeOnClient(mc -> {
            var doc = ((HudEditorScreen) mc.screen).document();
            var seg = doc.find("style-paint");
            var r = dev.henny.vui.layout.ScreenRects.of(seg);
            return new float[] {r.right() - 12, r.centerY()};
        });
        t.click(rainbow[0], rainbow[1]);
        t.context.waitTicks(6);
        HudStyle.TextPaint paint = t.context.computeOnClient(mc -> HudLayout.placement(HudLayout.get("hud.effects")).style.paint);
        check(paint == HudStyle.TextPaint.RAINBOW, "style panel did not set rainbow: " + paint);
        t.shot("hud_10_style_rainbow");
        t.context.runOnClient(mc -> ((HudEditorScreen) mc.screen).controller().styleTab("size"));
        t.context.waitTicks(4);
        t.shot("hud_10_style_size");
        t.key(Keys.ESCAPE);
        check(panel(t).isEmpty(), "Esc did not close the style panel");
    }

    private static void library(HudTestKit t) {
        t.click("dock-widgets");
        t.context.waitTicks(6);
        check("widgets".equals(panel(t)), "widgets panel: " + panel(t));
        t.shot("hud_11_library");
        t.key(Keys.ESCAPE);
    }

    private static void presets(HudTestKit t) {
        t.click("dock-presets");
        t.context.waitTicks(5);
        t.shot("hud_12_presets");
        t.click("preset-building");
        t.context.waitTicks(6);
        check(!t.context.computeOnClient(mc -> HudLayout.hidden(HudLayout.get("hud.compass"))), "building preset did not show the compass");
        check(t.context.computeOnClient(mc -> HudLayout.hidden(HudLayout.get("hud.keystrokes"))), "building preset kept the keystrokes");
        t.shot("hud_12_preset_building");
        t.ctrl('Z', false);
        check(!t.context.computeOnClient(mc -> HudLayout.hidden(HudLayout.get("hud.keystrokes"))), "undo did not restore the pvp preset");
    }

    private static void shareImport(HudTestKit t) {
        HudLayout.State want = t.context.computeOnClient(mc -> {
            dev.henny.vibeyclient.ui.hud.HudTemplates.apply("streamer");
            HudLayout.State s = HudLayout.capture();
            ClientCompat.setClipboard(HudShare.exportCode());
            dev.henny.vibeyclient.ui.hud.HudTemplates.apply("pvp");
            ((HudEditorScreen) mc.screen).refresh();
            return s;
        });
        t.context.waitTicks(3);
        t.click("dock-share");
        t.context.waitTicks(4);
        t.shot("hud_13_share_menu");
        t.click("share-import");
        t.context.waitTicks(5);
        check("import".equals(panel(t)), "import dialog: " + panel(t));
        t.shot("hud_13_import_dialog");
        t.click("import-go");
        t.context.waitTicks(5);
        check(t.context.computeOnClient(mc -> want.sameAs(HudLayout.capture())), "imported share code differs");
        t.shot("hud_13_imported_streamer");
        t.ctrl('Z', false);
    }

    private static void duplicate(HudTestKit t) {
        t.context.runOnClient(mc -> ((HudEditorScreen) mc.screen).select("hud.items"));
        t.ctrl('D', false);
        check(t.context.computeOnClient(mc -> HudLayout.get("hud.items.3") != null), "Ctrl+D did not duplicate the item counter");
        t.shot("hud_14_duplicated");
    }
}
