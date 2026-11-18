package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.ui.hud.Anchor;
import dev.henny.vibeyclient.ui.hud.HudEditorScreen;
import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.hud.HudShare;
import dev.henny.vibeyclient.ui.hud.HudStyle;
import dev.henny.vibeyclient.ui.hud.HudTemplates;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Locale;

/**
 * The HUD editor and widgets (Fabric 1.21.11): first-run intro, default template, every template in a world,
 * dragging with guides, handle resize (proportional and Shift = free), marquee multi-select, undo/redo, context
 * menu, style popover + colour picker, library, a widget the server disabled, share code round trip and bad input,
 * and the layout at 1280×720 / 1920×1080 with GUI scale 2, 3 and auto. Also records frames for a short video
 * (hudvid_*). Screenshots: versions/fabric-1.21.11/build/run/clientGameTest/screenshots (hud_*).
 */
public final class HudGameTest implements FabricClientGameTest {
    private static final String LOG = "[vibeyclient-hud-test] ";

    @Override
    public void runTest(ClientGameTestContext context) {
        context.getInput().resizeWindow(1280, 720);
        guiScale(context, 2);
        context.runOnClient(mc -> {
            GlobalSettings.language.set(GlobalSettings.Language.ENGLISH);
            if (!ModuleManager.get().isEnabled("hud")) throw new AssertionError("hud module off: " + ModuleManager.get().get("hud").status());
            HudLayout.setFlag("intro", false);
            for (String k : List.of("hud.fps", "hud.cps", "hud.keystrokes", "hud.coords", "hud.armor", "hud.effects", "hud.clock",
                    "hud.ping", "hud.server", "hud.speed", "hud.time", "hud.items", "hud.memory", "hud.compass")) {
                if (!ServerRules.supportedKeys().contains(k)) throw new AssertionError("rules key not declared: " + k);
                if (HudLayout.get(k) == null) throw new AssertionError("widget missing: " + k);
            }
            // the first start applied the default template: not an empty HUD
            if (HudLayout.hidden(HudLayout.get("hud.fps"))) throw new AssertionError("default template not applied");
        });
        shareCodes(context);

        try (TestSingleplayerContext world = context.worldBuilder()
                .setUseConsistentSettings(false)
                .adjustSettings(s -> s.setSeed("vibeyhud"))
                .create()) {
            world.getServer().runCommand("time set 1500");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("gamerule doDaylightCycle false");
            world.getServer().runCommand("gamerule announceAdvancements false");
            world.getServer().runCommand("item replace entity @a armor.head with minecraft:diamond_helmet");
            world.getServer().runCommand("item replace entity @a armor.chest with minecraft:netherite_chestplate");
            world.getServer().runCommand("item replace entity @a armor.legs with minecraft:iron_leggings");
            world.getServer().runCommand("item replace entity @a armor.feet with minecraft:diamond_boots");
            world.getServer().runCommand("give @a minecraft:arrow 48");
            world.getServer().runCommand("give @a minecraft:totem_of_undying 2");
            world.getServer().runCommand("effect give @a minecraft:speed 300 1 true");
            world.getServer().runCommand("effect give @a minecraft:night_vision 300 0 true");
            world.getServer().runCommand("effect give @a minecraft:strength 9 0 true");
            world.getClientWorld().waitForChunksRender();
            // up in the air over the landscape: a calm, bright background for the screenshots
            world.getServer().runCommand("gamerule fallDamage false");
            world.getServer().runCommand("gamerule keepInventory true");
            int[] at = context.computeOnClient(mc -> new int[] {
                    (int) Math.floor(mc.player.getX()), (int) Math.floor(mc.player.getY()), (int) Math.floor(mc.player.getZ())});
            int top = at[1] + 24;
            world.getServer().runCommand(String.format("fill %d %d %d %d %d %d minecraft:glass", at[0] - 1, top, at[2] - 1, at[0] + 1, top, at[2] + 1));
            world.getServer().runCommand(String.format("tp @a %d.5 %d %d.5", at[0], top + 1, at[2]));
            context.waitTicks(20);
            world.getClientWorld().waitForChunksRender();
            context.runOnClient(mc -> {
                mc.options.hideGui = false;
                mc.player.setYRot(135f);
                mc.player.setXRot(-4f);
                // damage the armour a bit so the durability shows
                for (var slot : new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS}) {
                    var st = mc.player.getItemBySlot(slot);
                    st.setDamageValue(st.getMaxDamage() / (slot == net.minecraft.world.entity.EquipmentSlot.CHEST ? 4 : 2));
                }
            });
            context.waitTicks(40);
            context.takeScreenshot("hud_01_default_minimal_ingame");

            intro(context);
            templates(context);
            editor(context);
            serverDisabled(context);
            resolutions(context);
            video(context);
        }
        context.runOnClient(mc -> {
            ServerRules.clear();
            HudEditorScreen.drawPointer = false;
            HudLayout.resetAll();
            HudTemplates.apply("minimal");
            HudLayout.save();
        });
    }

    // ============================================================================================ intro

    private static void intro(ClientGameTestContext context) {
        context.setScreen(() -> new HudEditorScreen(null));
        context.waitTicks(10);
        context.takeScreenshot("hud_02_intro_1_drag");
        click(context, "intro next", () -> introButton(context));
        context.waitTicks(12);
        context.takeScreenshot("hud_02_intro_2_resize");
        click(context, "intro next", () -> introButton(context));
        context.waitTicks(20);
        context.takeScreenshot("hud_02_intro_3_style");
        click(context, "intro done", () -> introButton(context));
        context.waitTicks(4);
        if (!context.computeOnClient(mc -> HudLayout.flag("intro"))) throw new AssertionError("intro not marked as seen");
        context.setScreen(() -> null);
        context.waitTicks(2);
    }

    /** The intro's primary button (bottom right of the 250 px card). */
    private static double[] introButton(ClientGameTestContext context) {
        return context.computeOnClient(mc -> {
            int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
            int w = Math.min(250, sw - 20), h = 150;
            int x = (sw - w) / 2, y = (sh - h) / 2;
            return new double[] {x + w - 20, y + h - 16};
        });
    }

    // ============================================================================================ templates

    private static void templates(ClientGameTestContext context) {
        for (String id : List.of("pvp", "minimal", "building", "streamer")) {
            boolean ok = context.computeOnClient(mc -> HudTemplates.apply(id));
            if (!ok) throw new AssertionError("template failed: " + id);
            context.waitTicks(8);
            context.takeScreenshot("hud_03_template_" + id);
        }
        // German names
        context.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.GERMAN));
        context.runOnClient(mc -> HudTemplates.apply("building"));
        context.waitTicks(6);
        context.takeScreenshot("hud_03_template_building_de");
        context.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.ENGLISH));
    }

    // ============================================================================================ editor

    private static void editor(ClientGameTestContext context) {
        context.runOnClient(mc -> HudTemplates.apply("pvp"));
        context.setScreen(() -> new HudEditorScreen(null));
        context.waitTicks(8);
        context.takeScreenshot("hud_04_editor_pvp");
        double gs = context.computeOnClient(mc -> mc.getWindow().getGuiScale());

        // drag the clock-free FPS counter towards the top centre: it snaps to the centre line (guide + readouts)
        HudElement fps = el(context, "hud.fps");
        HudLayout.Box b = box(context, fps);
        int sw = context.computeOnClient(mc -> mc.getWindow().getGuiScaledWidth());
        double sx = b.x + b.w / 2, sy = b.y + b.h / 2;
        double tx = sw / 2.0 + 1.5, ty = sy + 30;
        context.getInput().setCursorPos(sx * gs, sy * gs);
        context.waitTicks(2);
        context.getInput().holdMouse(0);
        context.waitTicks(1);
        for (int i = 1; i <= 12; i++) {
            context.getInput().setCursorPos((sx + (tx - sx) * i / 12) * gs, (sy + (ty - sy) * i / 12) * gs);
            context.waitTicks(1);
        }
        context.takeScreenshot("hud_05_drag_guides_centre");
        context.getInput().releaseMouse(0);
        context.waitTicks(2);
        HudLayout.Box moved = box(context, fps);
        float centre = moved.x + moved.w / 2;
        System.out.printf(Locale.ROOT, LOG + "fps moved to %.1f,%.1f (centre %.1f of %d), anchor %s%n", moved.x, moved.y, centre, sw,
                context.computeOnClient(mc -> HudLayout.placement(fps).anchor));
        if (Math.abs(centre - sw / 2f) > 1.01f) throw new AssertionError("fps did not snap to the centre: " + centre);
        if (context.computeOnClient(mc -> HudLayout.placement(fps).anchor) != Anchor.TOP) throw new AssertionError("anchor not TOP");

        // undo / redo with the keys of the player's keyboard layout (compares the stored placement)
        String movedP = placementOf(context, fps);
        pressCtrl(context, 'Z', false);
        String undoneP = placementOf(context, fps);
        if (undoneP.equals(movedP)) throw new AssertionError("Ctrl+Z did nothing");
        pressCtrl(context, 'Y', false);
        expectPlacement(context, fps, movedP, "Ctrl+Y");
        pressCtrl(context, 'Z', false);
        expectPlacement(context, fps, undoneP, "Ctrl+Z again");
        pressCtrl(context, 'Z', true);
        expectPlacement(context, fps, movedP, "Ctrl+Shift+Z");
        System.out.println(LOG + "undo/redo ok: " + undoneP + " <-> " + movedP);

        // arrow keys nudge the selection
        context.runOnClient(mc -> ((HudEditorScreen) mc.screen).select("hud.ping"));
        HudLayout.Box p0 = box(context, el(context, "hud.ping"));
        context.getInput().pressKey(Keys.toNative(Keys.RIGHT));
        context.getInput().holdKey(Keys.toNative(Keys.LEFT_SHIFT));
        context.getInput().pressKey(Keys.toNative(Keys.DOWN));
        context.getInput().releaseKey(Keys.toNative(Keys.LEFT_SHIFT));
        context.waitTicks(2);
        HudLayout.Box p1 = box(context, el(context, "hud.ping"));
        if (Math.round(p1.x - p0.x) != 1 || Math.round(p1.y - p0.y) != 10) throw new AssertionError("nudge: " + (p1.x - p0.x) + "," + (p1.y - p0.y));

        // resize the armour with its bottom left handle (proportional)
        HudElement armor = el(context, "hud.armor");
        context.runOnClient(mc -> ((HudEditorScreen) mc.screen).select("hud.armor"));
        context.waitTicks(2);
        HudLayout.Box ab = box(context, armor);
        float s0 = context.computeOnClient(mc -> HudLayout.placement(armor).scale);
        double hx = ab.x - 1, hy = ab.y + ab.h;
        context.getInput().setCursorPos(hx * gs, hy * gs);
        context.waitTicks(2);
        context.getInput().holdMouse(0);
        for (int i = 1; i <= 10; i++) {
            context.getInput().setCursorPos((hx - 3 * i) * gs, (hy + 3 * i) * gs);
            context.waitTicks(1);
        }
        context.takeScreenshot("hud_06_resize_handles");
        context.getInput().releaseMouse(0);
        context.waitTicks(2);
        float s1 = context.computeOnClient(mc -> HudLayout.placement(armor).scale);
        HudLayout.Box ab1 = box(context, armor);
        System.out.printf(Locale.ROOT, LOG + "armor scale %.2f -> %.2f, right edge %.1f -> %.1f%n", s0, s1, ab.x + ab.w, ab1.x + ab1.w);
        if (s1 < s0 + 0.2f) throw new AssertionError("armor did not grow: " + s1);
        if (Math.abs((ab1.x + ab1.w) - (ab.x + ab.w)) > 1.5f) throw new AssertionError("right edge moved while resizing from the left");

        // free resize (Shift) of the compass: only the width changes
        context.runOnClient(mc -> {
            HudLayout.setHidden(el2("hud.compass"), false);
            ((HudEditorScreen) mc.screen).select("hud.compass");
        });
        context.waitTicks(2);
        HudElement compass = el(context, "hud.compass");
        HudLayout.Box cb = box(context, compass);
        double rx = cb.x + cb.w, ry = cb.y + cb.h / 2;
        context.getInput().setCursorPos(rx * gs, ry * gs);
        context.waitTicks(2);
        context.getInput().holdKey(Keys.toNative(Keys.LEFT_SHIFT));
        context.getInput().holdMouse(0);
        for (int i = 1; i <= 8; i++) {
            context.getInput().setCursorPos((rx + 7 * i) * gs, (ry + 2 * i) * gs);
            context.waitTicks(1);
        }
        context.takeScreenshot("hud_06_resize_free_shift");
        context.getInput().releaseMouse(0);
        context.getInput().releaseKey(Keys.toNative(Keys.LEFT_SHIFT));
        context.waitTicks(2);
        HudLayout.Box cb1 = box(context, compass);
        System.out.printf(Locale.ROOT, LOG + "compass %.0fx%.0f -> %.0fx%.0f%n", cb.w, cb.h, cb1.w, cb1.h);
        if (cb1.w < cb.w + 30 || Math.abs(cb1.h - cb.h) > 0.5f) throw new AssertionError("free resize wrong: " + cb1.w + "x" + cb1.h);

        // marquee: select the left column (FPS, CPS, ping) and move them together
        context.runOnClient(mc -> ((HudEditorScreen) mc.screen).select(null));
        HudLayout.Box cps = box(context, el(context, "hud.cps"));
        HudLayout.Box ping = box(context, el(context, "hud.ping"));
        double mx0 = cps.x + cps.w + 30, my0 = cps.y - 3;
        double mx1 = Math.max(1, cps.x - 2), my1 = ping.y + ping.h + 2;
        context.getInput().setCursorPos(mx0 * gs, my0 * gs);
        context.waitTicks(2);
        context.getInput().holdMouse(0);
        for (int i = 1; i <= 6; i++) {
            context.getInput().setCursorPos((mx0 + (mx1 - mx0) * i / 6) * gs, (my0 + (my1 - my0) * i / 6) * gs);
            context.waitTicks(1);
        }
        context.takeScreenshot("hud_07_marquee");
        context.getInput().releaseMouse(0);
        context.waitTicks(3);
        context.takeScreenshot("hud_07_multiselect");
        int selected = context.computeOnClient(mc -> ((HudEditorScreen) mc.screen).selectionCount());
        System.out.println(LOG + "marquee selected " + selected);
        if (selected < 2) throw new AssertionError("marquee selected " + selected);
        // move the group
        HudLayout.Box c0 = box(context, el(context, "hud.cps"));
        HudLayout.Box q0 = box(context, el(context, "hud.ping"));
        double gx = c0.x + c0.w / 2, gy = c0.y + c0.h / 2;
        context.getInput().setCursorPos(gx * gs, gy * gs);
        context.waitTicks(1);
        context.getInput().holdMouse(0);
        for (int i = 1; i <= 6; i++) {
            context.getInput().setCursorPos((gx + 9 * i) * gs, (gy + 5 * i) * gs);
            context.waitTicks(1);
        }
        context.takeScreenshot("hud_07_group_move");
        context.getInput().releaseMouse(0);
        context.waitTicks(2);
        HudLayout.Box c1 = box(context, el(context, "hud.cps"));
        HudLayout.Box q1 = box(context, el(context, "hud.ping"));
        if (Math.abs((c1.x - c0.x) - (q1.x - q0.x)) > 0.5f || Math.abs((c1.y - c0.y) - (q1.y - q0.y)) > 0.5f || c1.x - c0.x < 20) {
            throw new AssertionError("group did not move together");
        }
        pressCtrl(context, 'Z', false);

        // right-click menu, then the style popover pages and the colour picker
        HudElement clock = el(context, "hud.effects");
        HudLayout.Box kb = box(context, clock);
        context.getInput().setCursorPos((kb.x + kb.w / 2) * gs, (kb.y + kb.h / 2) * gs);
        context.waitTicks(2);
        context.getInput().pressMouse(1);
        context.waitTicks(4);
        context.takeScreenshot("hud_08_context_menu");
        // the first item is "Style…"
        context.getInput().setCursorPos((kb.x + kb.w / 2 + 30) * gs, (kb.y + kb.h / 2 + 2 + 3 + 13 + 7) * gs);
        context.waitTicks(2);
        context.getInput().pressMouse(0);
        context.getInput().setCursorPos(320 * gs, 250 * gs);
        context.waitTicks(6);
        context.takeScreenshot("hud_09_style_box");
        context.runOnClient(mc -> ((HudEditorScreen) mc.screen).testOpen("styleText", 0, 0));
        context.waitTicks(4);
        context.takeScreenshot("hud_09_style_text");
        context.runOnClient(mc -> ((HudEditorScreen) mc.screen).testOpen("styleSize", 0, 0));
        context.waitTicks(4);
        context.takeScreenshot("hud_09_style_size");
        context.runOnClient(mc -> ((HudEditorScreen) mc.screen).testOpen("picker", 0, 0));
        context.waitTicks(4);
        context.takeScreenshot("hud_10_colour_picker");
        // style edits: rainbow text + bevel on the clock, and a global accent
        context.runOnClient(mc -> {
            HudLayout.Placement p = HudLayout.placement(el2("hud.fps"));
            p.style.paint = HudStyle.TextPaint.RAINBOW;
            HudLayout.globalStyle().accentColor = 0x3FE0C5;
            HudLayout.styleChanged();
            ((HudEditorScreen) mc.screen).select("hud.fps");
            ((HudEditorScreen) mc.screen).testOpen("styleText", 0, 0);
        });
        context.waitTicks(6);
        context.takeScreenshot("hud_09_style_rainbow_override");
        context.getInput().pressKey(Keys.toNative(Keys.ESCAPE));
        context.waitTicks(2);

        // library, templates and share menus, import dialog
        context.runOnClient(mc -> ((HudEditorScreen) mc.screen).testOpen("library", 0, 0));
        context.waitTicks(8);
        context.takeScreenshot("hud_11_library");
        context.runOnClient(mc -> ((HudEditorScreen) mc.screen).testOpen("templates", 0, 0));
        context.waitTicks(4);
        context.takeScreenshot("hud_11_templates_menu");
        context.getInput().pressKey(Keys.toNative(Keys.ESCAPE));
        context.getInput().pressKey(Keys.toNative(Keys.ESCAPE));
        context.runOnClient(mc -> ((HudEditorScreen) mc.screen).testOpen("share", 0, 0));
        context.waitTicks(4);
        context.takeScreenshot("hud_11_share_menu");
        context.getInput().pressKey(Keys.toNative(Keys.ESCAPE));
        context.runOnClient(mc -> {
            ClientCompat.setClipboard(HudShare.exportCode());
            ((HudEditorScreen) mc.screen).testOpen("import", 0, 0);
        });
        context.waitTicks(4);
        context.takeScreenshot("hud_11_import_dialog");
        context.getInput().pressKey(Keys.toNative(Keys.ESCAPE));
        context.waitTicks(2);

        // duplicate an item counter (Ctrl+D) and keep it
        context.runOnClient(mc -> ((HudEditorScreen) mc.screen).select("hud.items"));
        pressCtrl(context, 'D', false);
        boolean dup = context.computeOnClient(mc -> HudLayout.get("hud.items.3") != null);
        if (!dup) throw new AssertionError("Ctrl+D did not duplicate the item counter");
        context.takeScreenshot("hud_12_duplicated");

        // Esc (with nothing open) saves and closes
        context.runOnClient(mc -> ((HudEditorScreen) mc.screen).select(null));
        context.getInput().pressKey(Keys.toNative(Keys.ESCAPE));
        context.waitTicks(12);
        if (context.computeOnClient(mc -> mc.screen) != null) throw new AssertionError("editor did not close");
        context.takeScreenshot("hud_12_result_ingame");
    }

    // ============================================================================================ server rules

    private static void serverDisabled(ClientGameTestContext context) {
        context.runOnClient(mc -> {
            HudTemplates.apply("building");
            ServerRules.apply(List.of("hud.coords"), "No coordinates in this event");
            if (!HudLayout.serverDisabled(el2("hud.coords"))) throw new AssertionError("coords not disabled");
            if (HudLayout.serverDisabled(el2("hud.fps"))) throw new AssertionError("fps disabled by mistake");
        });
        context.waitTicks(4);
        context.takeScreenshot("hud_13_server_disabled_ingame");
        context.setScreen(() -> new HudEditorScreen(null));
        context.waitTicks(8);
        context.takeScreenshot("hud_13_server_disabled_editor");
        context.runOnClient(mc -> ((HudEditorScreen) mc.screen).testOpen("library", 0, 0));
        context.waitTicks(8);
        context.takeScreenshot("hud_13_server_disabled_library");
        context.setScreen(() -> null);
        context.runOnClient(mc -> ServerRules.apply(List.of("hud.*"), null));
        context.waitTicks(3);
        context.takeScreenshot("hud_13_server_disabled_all");
        context.runOnClient(mc -> ServerRules.clear());
    }

    // ============================================================================================ resolutions

    private static void resolutions(ClientGameTestContext context) {
        context.runOnClient(mc -> HudTemplates.apply("pvp"));
        int[][] sizes = {{1280, 720}, {1920, 1080}};
        int[] scales = {2, 3, 0};
        for (int[] s : sizes) {
            context.getInput().resizeWindow(s[0], s[1]);
            for (int gs : scales) {
                guiScale(context, gs);
                context.waitTicks(6);
                String name = "hud_14_res_" + s[0] + "x" + s[1] + "_gui" + (gs == 0 ? "auto" : gs);
                context.takeScreenshot(name);
                // corner widgets keep their distance to their corner
                float[] d = context.computeOnClient(mc -> {
                    int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
                    HudLayout.Box fx = HudLayout.box(el2("hud.fps"), sw, sh, new HudLayout.Box());
                    HudLayout.Box ef = HudLayout.box(el2("hud.effects"), sw, sh, new HudLayout.Box());
                    HudLayout.Box ar = HudLayout.box(el2("hud.armor"), sw, sh, new HudLayout.Box());
                    return new float[] {fx.x, fx.y, sw - ef.x - ef.w, ef.y, sw - ar.x - ar.w, (ar.y + ar.h / 2) - sh / 2f, sw, sh};
                });
                System.out.printf(Locale.ROOT, LOG + "%s gui %.0fx%.0f fps@%.0f,%.0f effects right %.0f top %.0f armor right %.0f centreOff %.1f%n",
                        name, d[6], d[7], d[0], d[1], d[2], d[3], d[4], d[5]);
                if (d[0] != 4 || d[1] != 4 || Math.abs(d[2] - 4) > 0.5f || Math.abs(d[4] - 4) > 0.5f || Math.abs(d[5]) > 1f) {
                    throw new AssertionError("layout did not keep its anchors at " + name);
                }
            }
        }
        context.getInput().resizeWindow(1280, 720);
        guiScale(context, 2);
        context.setScreen(() -> new HudEditorScreen(null));
        context.getInput().resizeWindow(1920, 1080);
        guiScale(context, 3);
        context.waitTicks(6);
        context.takeScreenshot("hud_14_editor_1920x1080_gui3");
        context.setScreen(() -> null);
        context.getInput().resizeWindow(1280, 720);
        guiScale(context, 2);
        context.waitTicks(4);
    }

    // ============================================================================================ video frames

    /** A scripted editor session, one screenshot per tick (encoded into hud-editor.mp4 afterwards). */
    private static void video(ClientGameTestContext context) {
        context.runOnClient(mc -> {
            HudTemplates.apply("pvp");
            HudEditorScreen.drawPointer = true;
        });
        context.setScreen(() -> new HudEditorScreen(null));
        context.waitTicks(6);
        double gs = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
        int sw = context.computeOnClient(mc -> mc.getWindow().getGuiScaledWidth());
        int[] frame = {0};
        Runnable shot = () -> context.takeScreenshot(String.format(Locale.ROOT, "hudvid_%04d", frame[0]++));
        // 1) glide to the FPS counter, drag it to the top centre (centre guide), then to the right edge
        HudLayout.Box f = box(context, el(context, "hud.fps"));
        double[] cur = {sw / 2.0, 120};
        moveCursor(context, gs, cur, f.x + f.w / 2, f.y + f.h / 2, 10, shot);
        context.getInput().holdMouse(0);
        hold(context, 2, shot);
        moveCursor(context, gs, cur, sw / 2.0 + 2, f.y + f.h / 2 + 2, 22, shot);
        hold(context, 8, shot);
        moveCursor(context, gs, cur, sw / 2.0 + 40, f.y + 22, 14, shot);
        hold(context, 4, shot);
        context.getInput().releaseMouse(0);
        hold(context, 4, shot);
        // 2) pull the armour's corner handle out (proportional resize)
        context.runOnClient(mc -> ((HudEditorScreen) mc.screen).select("hud.armor"));
        HudLayout.Box a = box(context, el(context, "hud.armor"));
        moveCursor(context, gs, cur, a.x - 1, a.y + a.h, 12, shot);
        context.getInput().holdMouse(0);
        hold(context, 2, shot);
        moveCursor(context, gs, cur, a.x - 26, a.y + a.h + 26, 18, shot);
        hold(context, 6, shot);
        context.getInput().releaseMouse(0);
        hold(context, 3, shot);
        // 3) drag the ping next to the CPS (snaps to its edge, distance readouts)
        HudLayout.Box p = box(context, el(context, "hud.ping"));
        HudLayout.Box c = box(context, el(context, "hud.cps"));
        moveCursor(context, gs, cur, p.x + p.w / 2, p.y + p.h / 2, 10, shot);
        context.getInput().holdMouse(0);
        hold(context, 2, shot);
        moveCursor(context, gs, cur, c.x + c.w + p.w / 2 + 4, c.y + c.h / 2 + 1, 18, shot);
        hold(context, 8, shot);
        context.getInput().releaseMouse(0);
        hold(context, 3, shot);
        // 4) marquee over the keystrokes and the CPS, move both
        HudLayout.Box k = box(context, el(context, "hud.keystrokes"));
        moveCursor(context, gs, cur, k.x + k.w + 50, k.y + k.h + 6, 10, shot);
        context.getInput().holdMouse(0);
        moveCursor(context, gs, cur, 1, c.y - 2, 14, shot);
        context.getInput().releaseMouse(0);
        hold(context, 4, shot);
        moveCursor(context, gs, cur, k.x + k.w / 2, k.y + k.h / 2, 8, shot);
        context.getInput().holdMouse(0);
        moveCursor(context, gs, cur, k.x + k.w / 2 + 40, k.y + k.h / 2 + 10, 14, shot);
        hold(context, 4, shot);
        context.getInput().releaseMouse(0);
        hold(context, 4, shot);
        // 5) right-click → style popover, flip on rainbow text
        HudLayout.Box ck = box(context, el(context, "hud.fps"));
        moveCursor(context, gs, cur, ck.x + ck.w / 2, ck.y + ck.h / 2, 8, shot);
        context.getInput().pressMouse(1);
        hold(context, 6, shot);
        moveCursor(context, gs, cur, ck.x + ck.w / 2 + 30, ck.y + ck.h / 2 + 25, 6, shot);
        context.getInput().pressMouse(0);
        hold(context, 6, shot);
        context.runOnClient(mc -> {
            ((HudEditorScreen) mc.screen).testOpen("styleText", 0, 0);
            HudLayout.placement(el2("hud.fps")).style.paint = HudStyle.TextPaint.RAINBOW;
            HudLayout.styleChanged();
        });
        hold(context, 20, shot);
        System.out.println(LOG + "video frames: " + frame[0]);
        context.setScreen(() -> null);
        context.runOnClient(mc -> HudEditorScreen.drawPointer = false);
    }

    private static void moveCursor(ClientGameTestContext context, double gs, double[] cur, double x, double y, int steps, Runnable shot) {
        double x0 = cur[0], y0 = cur[1];
        for (int i = 1; i <= steps; i++) {
            double t = i / (double) steps;
            double e = t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
            cur[0] = x0 + (x - x0) * e;
            cur[1] = y0 + (y - y0) * e;
            context.getInput().setCursorPos(cur[0] * gs, cur[1] * gs);
            context.waitTick();
            shot.run();
        }
    }

    private static void hold(ClientGameTestContext context, int ticks, Runnable shot) {
        for (int i = 0; i < ticks; i++) {
            context.waitTick();
            shot.run();
        }
    }

    // ============================================================================================ share codes

    private static void shareCodes(ClientGameTestContext context) {
        context.runOnClient(mc -> {
            HudTemplates.apply("streamer");
            HudLayout.State before = HudLayout.capture();
            String code = HudShare.exportCode();
            System.out.println(LOG + "share code (" + code.length() + " chars): " + code.substring(0, Math.min(60, code.length())) + "…");
            HudLayout.resetAll();
            try {
                HudLayout.apply(HudShare.importCode(code), true);
            } catch (HudShare.BadLayout e) {
                throw new AssertionError("own code rejected: " + e.key);
            }
            if (!before.sameAs(HudLayout.capture())) throw new AssertionError("share code round trip changed the layout");
            for (String bad : List.of("", "hello", "VHUD1.", "VHUD1.!!!!", "VHUD9.abc", "VHUD1.eJwLycgsVgCi4pTUvHQA",
                    "{\"v\":1}", "{\"v\":99,\"e\":{}}", "{\"e\":[1,2]}", "{".repeat(5000), "VHUD1." + "A".repeat(30000),
                    "{\"v\":1,\"e\":{\"hud.fps\":{\"a\":\"nowhere\",\"x\":1e999,\"s\":\"big\",\"st\":{\"tc\":\"zzzzzz\"}}}}")) {
                try {
                    HudLayout.State s = HudShare.importCode(bad);
                    // a code that parses must still be safe to apply
                    HudLayout.apply(s, true);
                    System.out.println(LOG + "accepted (sanitised): " + bad.substring(0, Math.min(40, bad.length())));
                } catch (HudShare.BadLayout e) {
                    System.out.println(LOG + "rejected " + e.key + ": " + bad.substring(0, Math.min(30, bad.length())));
                } catch (RuntimeException e) {
                    throw new AssertionError("import crashed on bad input: " + bad.substring(0, Math.min(40, bad.length())), e);
                }
            }
            HudTemplates.apply("minimal");
        });
    }

    // ============================================================================================ util

    private static String placementOf(ClientGameTestContext context, HudElement e) {
        return context.computeOnClient(mc -> {
            HudLayout.Placement p = HudLayout.placement(e);
            return p.anchor + " " + p.dx + "," + p.dy + " x" + p.scale;
        });
    }

    private static void expectPlacement(ClientGameTestContext context, HudElement e, String want, String what) {
        String now = placementOf(context, e);
        if (!now.equals(want)) throw new AssertionError(what + ": " + now + " instead of " + want);
    }

    private static void expectX(ClientGameTestContext context, HudElement e, float x, String what) {
        float now = box(context, e).x;
        if (Math.abs(now - x) > 0.5f) throw new AssertionError(what + ": x " + now + " instead of " + x);
    }

    private static void pressCtrl(ClientGameTestContext context, char letter, boolean shift) {
        int code = context.computeOnClient(mc -> {
            for (int c = 'A'; c <= 'Z'; c++) if (ClientCompat.layoutLetter(c) == letter) return c;
            return (int) letter;
        });
        context.getInput().holdKey(Keys.toNative(Keys.LEFT_CONTROL));
        if (shift) context.getInput().holdKey(Keys.toNative(Keys.LEFT_SHIFT));
        context.getInput().pressKey(Keys.toNative(code));
        if (shift) context.getInput().releaseKey(Keys.toNative(Keys.LEFT_SHIFT));
        context.getInput().releaseKey(Keys.toNative(Keys.LEFT_CONTROL));
        context.waitTicks(2);
    }

    private interface Pos {
        double[] get();
    }

    private static void click(ClientGameTestContext context, String what, Pos pos) {
        double gs = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
        double[] p = pos.get();
        context.getInput().setCursorPos(p[0] * gs, p[1] * gs);
        context.waitTicks(2);
        context.getInput().pressMouse(0);
        context.waitTicks(1);
    }

    private static HudElement el(ClientGameTestContext context, String id) {
        HudElement e = context.computeOnClient(mc -> HudLayout.get(id));
        if (e == null) throw new AssertionError("no element " + id);
        return e;
    }

    private static HudElement el2(String id) {
        HudElement e = HudLayout.get(id);
        if (e == null) throw new AssertionError("no element " + id);
        return e;
    }

    private static HudLayout.Box box(ClientGameTestContext context, HudElement e) {
        return context.computeOnClient(mc -> HudLayout.box(e, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), new HudLayout.Box()));
    }

    private static void guiScale(ClientGameTestContext context, int scale) {
        context.runOnClient(mc -> {
            mc.options.guiScale().set(scale);
            mc.resizeDisplay();
        });
    }

    @SuppressWarnings("unused")
    private static Minecraft mc() {
        return Minecraft.getInstance();
    }
}
