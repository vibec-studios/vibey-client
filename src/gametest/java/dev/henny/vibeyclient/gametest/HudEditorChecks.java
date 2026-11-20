package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.ui.hud.Anchor;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.hud.HudTemplates;
import dev.henny.vibeyclient.ui.hud.editor.HudEditorScreen;

import java.util.Locale;

import static dev.henny.vibeyclient.gametest.HudTestKit.check;

final class HudEditorChecks {
    private HudEditorChecks() {}

    static void run(HudTestKit t) {
        t.context.runOnClient(mc -> HudTemplates.apply("pvp"));
        t.openEditor();
        t.assertNoVuiErrors();
        t.shot("hud_02_editor_pvp");
        select(t);
        dragSnap(t);
        undoRedo(t);
        nudge(t);
        wheel(t);
        resize(t);
        multiSelect(t);
        hiddenNoSnap(t);
        HudEditorPanelsCheck.run(t);
        t.context.runOnClient(mc -> ((HudEditorScreen) mc.screen).select(null));
        t.key(Keys.ESCAPE);
        t.context.waitTicks(8);
        check(t.editor() == null, "Esc did not close the editor");
        t.shot("hud_19_result_ingame");
    }

    private static void select(HudTestKit t) {
        HudLayout.Box b = t.box("hud.fps");
        t.click(b.x + b.w / 2, b.y + b.h / 2);
        t.context.waitTicks(3);
        check(t.context.computeOnClient(mc -> ((HudEditorScreen) mc.screen).session().selection.primary().id().equals("hud.fps")), "click did not select fps");
        check(t.exists("wbar"), "selection bar missing");
        t.shot("hud_03_selected_bar");
    }

    private static void dragSnap(HudTestKit t) {
        HudLayout.Box b = t.box("hud.fps");
        double sx = b.x + b.w / 2, sy = b.y + b.h / 2;
        int sw = t.sw();
        int[] guides = {0};
        t.drag(sx, sy, sw / 2.0 + 1.5, sy + 30, 12, () -> {
            guides[0] = t.context.computeOnClient(mc -> ((HudEditorScreen) mc.screen).session().guides.lines().size());
            t.shot("hud_04_drag_snap_guide");
        });
        HudLayout.Box m = t.box("hud.fps");
        float centre = m.x + m.w / 2;
        System.out.printf(Locale.ROOT, HudTestKit.LOG + "fps centre %.1f of %d, guides %d%n", centre, sw, guides[0]);
        check(guides[0] > 0, "no snap guide while dragging");
        check(Math.abs(centre - sw / 2f) <= 1.01f, "fps did not snap to the centre: " + centre);
        check(t.context.computeOnClient(mc -> HudLayout.placement(HudLayout.get("hud.fps")).anchor) == Anchor.TOP, "anchor not TOP");
    }

    private static void undoRedo(HudTestKit t) {
        String moved = t.placement("hud.fps");
        t.ctrl('Z', false);
        String undone = t.placement("hud.fps");
        check(!undone.equals(moved), "Ctrl+Z did nothing");
        t.ctrl('Y', false);
        check(t.placement("hud.fps").equals(moved), "Ctrl+Y did not redo");
        t.ctrl('Z', false);
        check(t.placement("hud.fps").equals(undone), "Ctrl+Z again");
        t.ctrl('Z', true);
        check(t.placement("hud.fps").equals(moved), "Ctrl+Shift+Z did not redo");
    }

    private static void nudge(HudTestKit t) {
        t.context.runOnClient(mc -> ((HudEditorScreen) mc.screen).select("hud.ping"));
        HudLayout.Box p0 = t.box("hud.ping");
        t.key(Keys.RIGHT);
        t.context.getInput().holdKey(Keys.toNative(Keys.LEFT_SHIFT));
        t.key(Keys.DOWN);
        t.context.getInput().releaseKey(Keys.toNative(Keys.LEFT_SHIFT));
        HudLayout.Box p1 = t.box("hud.ping");
        check(Math.round(p1.x - p0.x) == 1 && Math.round(p1.y - p0.y) == 10, "nudge: " + (p1.x - p0.x) + "," + (p1.y - p0.y));
    }

    private static void wheel(HudTestKit t) {
        HudLayout.Box a = t.box("hud.armor");
        t.point(a.x + a.w / 2, a.y + a.h / 2);
        t.context.getInput().scroll(1);
        t.context.waitTicks(3);
        float s1 = t.context.computeOnClient(mc -> HudLayout.placement(HudLayout.get("hud.armor")).scale);
        check(Math.abs(s1 - 1.1f) < 0.01f, "wheel step 1 -> " + s1);
        t.context.getInput().scroll(1);
        t.context.waitTicks(4);
        String pct = t.context.computeOnClient(mc -> ((HudEditorScreen) mc.screen).controller().selected.peek().pct());
        check("125%".equals(pct), "bar shows " + pct);
        t.shot("hud_05_wheel_scale_pct");
        t.context.getInput().scroll(-1);
        t.context.getInput().scroll(-1);
        t.context.waitTicks(3);
    }

    private static void resize(HudTestKit t) {
        t.context.runOnClient(mc -> ((HudEditorScreen) mc.screen).select("hud.armor"));
        t.context.waitTicks(2);
        HudLayout.Box ab = t.box("hud.armor");
        float s0 = t.context.computeOnClient(mc -> HudLayout.placement(HudLayout.get("hud.armor")).scale);
        t.drag(ab.x - 1, ab.y + ab.h, ab.x - 31, ab.y + ab.h + 30, 10, () -> t.shot("hud_06_resize_handle"));
        float s1 = t.context.computeOnClient(mc -> HudLayout.placement(HudLayout.get("hud.armor")).scale);
        HudLayout.Box ab1 = t.box("hud.armor");
        check(s1 > s0 + 0.2f, "armor did not grow: " + s1);
        check(Math.abs((ab1.x + ab1.w) - (ab.x + ab.w)) <= 1.5f, "right edge moved while resizing from the left");
        t.ctrl('Z', false);
    }

    private static void multiSelect(HudTestKit t) {
        t.context.runOnClient(mc -> ((HudEditorScreen) mc.screen).select(null));
        HudLayout.Box cps = t.box("hud.cps");
        HudLayout.Box ping = t.box("hud.ping");
        t.drag(cps.x + cps.w + 30, cps.y - 3, Math.max(1, cps.x - 2), ping.y + ping.h + 2, 6, () -> t.shot("hud_07_marquee"));
        int n = t.context.computeOnClient(mc -> ((HudEditorScreen) mc.screen).session().selection.size());
        check(n >= 2, "marquee selected " + n);
        HudLayout.Box ks = t.box("hud.effects");
        t.context.getInput().holdKey(Keys.toNative(Keys.LEFT_CONTROL));
        t.click(ks.x + ks.w / 2, ks.y + ks.h / 2);
        t.context.getInput().releaseKey(Keys.toNative(Keys.LEFT_CONTROL));
        int n2 = t.context.computeOnClient(mc -> ((HudEditorScreen) mc.screen).session().selection.size());
        check(n2 == n + 1, "ctrl-click did not add: " + n2);
        t.shot("hud_08_multiselect");
        HudLayout.Box c0 = t.box("hud.cps");
        HudLayout.Box k0 = t.box("hud.effects");
        t.drag(c0.x + c0.w / 2, c0.y + c0.h / 2, c0.x + c0.w / 2 + 54, c0.y + c0.h / 2 + 20, 6, () -> t.shot("hud_08_group_move"));
        HudLayout.Box c1 = t.box("hud.cps");
        HudLayout.Box k1 = t.box("hud.effects");
        check(Math.abs((c1.y - c0.y) - (k1.y - k0.y)) <= 0.5f && Math.abs((c1.x - c0.x) - (k1.x - k0.x)) <= 0.5f && c1.y - c0.y > 10,
                "group did not move together: " + (c1.y - c0.y) + " vs " + (k1.y - k0.y));
        t.ctrl('Z', false);
    }

    private static void hiddenNoSnap(HudTestKit t) {
        t.context.runOnClient(mc -> {
            HudLayout.setHidden(HudLayout.get("hud.memory"), true);
            HudLayout.placement(HudLayout.get("hud.memory")).anchor = Anchor.TOP_LEFT;
            HudLayout.placement(HudLayout.get("hud.memory")).dx = 200;
            HudLayout.placement(HudLayout.get("hud.memory")).dy = 120;
            ((HudEditorScreen) mc.screen).select(null);
        });
        HudLayout.Box f = t.box("hud.fps");
        t.drag(f.x + 1, f.y + f.h / 2, 202 + 1, 140, 10, null);
        HudLayout.Box m = t.box("hud.fps");
        System.out.printf(Locale.ROOT, HudTestKit.LOG + "fps left %.1f (hidden memory at 200)%n", m.x);
        check(Math.abs(m.x - 202) <= 0.5f, "snapped to a hidden widget: " + m.x);
        t.ctrl('Z', false);
    }
}
