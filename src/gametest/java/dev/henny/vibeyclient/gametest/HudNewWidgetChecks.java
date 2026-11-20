package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.ui.hud.Anchor;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import dev.henny.vibeyclient.ui.hud.HudTemplates;
import dev.henny.vibeyclient.ui.hud.editor.HudEditorScreen;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import static dev.henny.vibeyclient.gametest.HudTestKit.check;

final class HudNewWidgetChecks {
    private HudNewWidgetChecks() {}

    static void run(HudTestKit t, TestSingleplayerContext world) {
        t.context.runOnClient(mc -> {
            HudTemplates.apply("minimal");
            for (String id : new String[] {"hud.timer", "hud.scoreboard", "hud.tablist", "hud.bossbar", "hud.title"}) HudLayout.setHidden(HudLayout.get(id), false);
        });
        for (String c : new String[] {"scoreboard objectives add kills dummy \"Kills\"", "scoreboard objectives setdisplay sidebar kills",
                "scoreboard players set Alex kills 7", "scoreboard players set Steve kills 3", "scoreboard players set @a kills 5",
                "bossbar add vibey:test \"Ender Dragon\"", "bossbar set vibey:test players @a", "bossbar set vibey:test color pink",
                "bossbar set vibey:test value 70"}) {
            world.getServer().runCommand(c);
        }
        t.context.waitTicks(10);
        timer(t);
        t.shot("hud_20_new_widgets_ingame");
        tabList(t);
        scoreboardNumbers(t);
        editorMoves(t, world);
    }

    private static void timer(HudTestKit t) {
        t.key(Keys.U);
        t.context.waitTicks(25);
        String text = t.context.computeOnClient(mc -> HudLayout.get("hud.timer").widget().naturalWidth() + "");
        check(t.context.computeOnClient(mc -> dev.henny.vibeyclient.module.hud.timer.Timers.running()), "U did not start the timer (" + text + ")");
        t.key(Keys.Y);
        t.context.waitTicks(10);
        t.key(Keys.Y);
        t.context.runOnClient(mc -> HudLayout.setOption(HudLayout.get("hud.timer"), "mode", "splits"));
        t.context.waitTicks(6);
        t.shot("hud_21_timer_splits");
        t.key(Keys.U);
        t.context.runOnClient(mc -> HudLayout.setOption(HudLayout.get("hud.timer"), "mode", "countdown"));
        t.key(Keys.Y);
    }

    private static void tabList(HudTestKit t) {
        t.context.getInput().holdKey(Keys.toNative(Keys.TAB));
        t.context.waitTicks(8);
        t.shot("hud_22_tablist");
        t.context.getInput().releaseKey(Keys.toNative(Keys.TAB));
        t.context.waitTicks(2);
    }

    private static void scoreboardNumbers(HudTestKit t) {
        t.context.runOnClient(mc -> HudLayout.setOption(HudLayout.get("hud.scoreboard"), "numbers", "0"));
        t.context.waitTicks(4);
        t.shot("hud_23_scoreboard_no_numbers");
    }

    private static void editorMoves(HudTestKit t, TestSingleplayerContext world) {
        t.openEditor();
        t.shot("hud_24_editor_new_widgets");
        HudLayout.Box tb = t.box("hud.timer");
        t.point(tb.x + tb.w / 2, tb.y + tb.h / 2);
        t.context.getInput().holdKey(Keys.toNative(Keys.LEFT_SHIFT));
        t.context.getInput().scroll(1);
        t.context.getInput().releaseKey(Keys.toNative(Keys.LEFT_SHIFT));
        t.context.waitTicks(4);
        String len = t.context.computeOnClient(mc -> HudLayout.placement(HudLayout.get("hud.timer")).opts.getOrDefault("length", "1m"));
        check("2m".equals(len), "Shift+wheel over the countdown: " + len);
        HudLayout.Box sb = t.box("hud.scoreboard");
        t.drag(sb.x + sb.w / 2, sb.y + 4, 60, 140, 10, null);
        t.point(60, 140);
        t.context.getInput().scroll(1);
        t.context.waitTicks(3);
        HudLayout.Box bb = t.box("hud.bossbar");
        t.drag(bb.x + 20, bb.y + bb.h / 2, t.sw() - 120, t.sh() - 70, 12, null);
        t.context.runOnClient(mc -> {
            HudLayout.placement(HudLayout.get("hud.title")).anchor = Anchor.TOP;
            HudLayout.placement(HudLayout.get("hud.title")).dy = 30;
            HudLayout.placement(HudLayout.get("hud.title")).scale = 0.5f;
            ((HudEditorScreen) mc.screen).select(null);
        });
        t.context.waitTicks(3);
        t.shot("hud_25_editor_moved");
        t.key(Keys.ESCAPE);
        t.context.waitTicks(4);
        world.getServer().runCommand("title @a times 10 200 10");
        world.getServer().runCommand("title @a subtitle \"moved and scaled\"");
        world.getServer().runCommand("title @a title \"Vibey\"");
        t.context.waitTicks(20);
        float bx = t.box("hud.bossbar").x;
        check(bx > t.sw() / 2f, "boss bar did not move: " + bx);
        t.shot("hud_26_moved_ingame");
        world.getServer().runCommand("bossbar remove vibey:test");
        world.getServer().runCommand("title @a clear");
        world.getServer().runCommand("scoreboard objectives remove kills");
    }
}
