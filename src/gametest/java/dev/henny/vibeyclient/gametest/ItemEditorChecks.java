package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.module.visuals.items.core.EditSession;
import dev.henny.vibeyclient.module.visuals.items.core.HandPose;
import dev.henny.vibeyclient.module.visuals.items.core.HandProjection;
import dev.henny.vibeyclient.module.visuals.items.edit.ItemEditScreen;
import dev.henny.vibeyclient.module.visuals.items.edit.ItemEditor;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import static dev.henny.vibeyclient.gametest.ItemModelsGameTest.KIT;
import static dev.henny.vibeyclient.gametest.ItemModelsGameTest.m;
import static dev.henny.vibeyclient.gametest.ReviewKit.LEFT;
import static dev.henny.vibeyclient.gametest.ReviewKit.RIGHT;
import static dev.henny.vibeyclient.gametest.ReviewKit.check;

final class ItemEditorChecks {
    private static final int ENTER = 257, ESCAPE = 256;

    private ItemEditorChecks() {}

    static void editor(ClientGameTestContext ctx, TestSingleplayerContext world) {
        ItemHeldChecks.hold(ctx, world, ItemHeldChecks.SWORD, "minecraft:torch");
        ctx.runOnClient(mc -> ItemEditScreen.drawPointer = true);
        open(ctx);
        float[] item = anchor(ctx);
        cursor(ctx, item[0], item[1]);
        KIT.shot(ctx, "editor_hover");
        ctx.getInput().holdMouse(LEFT);
        ctx.waitTicks(2);
        for (int i = 1; i <= 6; i++) cursor(ctx, item[0] - 12 * i, item[1] - 6 * i);
        KIT.shotNow(ctx, "editor_grab_drag");
        ctx.getInput().releaseMouse(LEFT);
        ctx.waitTicks(2);
        HandPose moved = pose(ctx);
        KIT.log("after drag: %s", moved);
        check(moved.x() < -0.05f && moved.y() > 0.02f, "drag should move the item left and up: " + moved);
        float[] now = anchor(ctx);
        cursor(ctx, now[0], now[1]);
        ctx.getInput().scroll(4);
        ctx.waitTicks(3);
        HandPose bigger = pose(ctx);
        check(bigger.scale() > 1.3f, "wheel up should scale the item: " + bigger);
        KIT.shot(ctx, "editor_wheel_bigger");
        ctx.getInput().pressMouse(RIGHT);
        ctx.waitTicks(4);
        check(ctx.computeOnClient(mc -> editScreen(mc).controller().panel.peek().equals("menu")), "right click should open the preset menu");
        KIT.shot(ctx, "editor_menu");
        ctx.getInput().pressKey(ESCAPE);
        ctx.waitTicks(3);
        check(ctx.computeOnClient(mc -> mc.screen instanceof ItemEditScreen), "Esc should only close the menu first");
        ctx.getInput().pressKey(ENTER);
        ctx.waitTicks(3);
        check(ctx.computeOnClient(mc -> mc.screen == null && ItemEditor.session() == null), "Enter should save and close");
        HandPose saved = ctx.computeOnClient(mc -> m().held.main.pose());
        check(close(saved, bigger), "Enter should store the pose: " + saved + " vs " + bigger);
        KIT.shot(ctx, "editor_saved_in_game");
        open(ctx);
        ctx.getInput().scroll(-6);
        ctx.waitTicks(2);
        ctx.getInput().pressKey(ESCAPE);
        ctx.waitTicks(3);
        HandPose kept = ctx.computeOnClient(mc -> m().held.main.pose());
        check(ctx.computeOnClient(mc -> mc.screen == null) && close(kept, saved), "Esc should cancel: " + kept);
        ctx.runOnClient(mc -> ItemEditScreen.drawPointer = false);
        ItemModelsGameTest.reset(ctx);
    }

    private static boolean close(HandPose a, HandPose b) {
        return Math.abs(a.x() - b.x()) < 0.01f && Math.abs(a.y() - b.y()) < 0.01f && Math.abs(a.scale() - b.scale()) < 0.011f;
    }

    private static void open(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> ItemEditor.open());
        ctx.waitTicks(6);
        check(ctx.computeOnClient(mc -> mc.screen instanceof ItemEditScreen), "editor should open");
    }

    private static ItemEditScreen editScreen(net.minecraft.client.Minecraft mc) {
        return (ItemEditScreen) mc.screen;
    }

    private static HandPose pose(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> ItemEditor.session().pose(EditSession.MAIN));
    }

    private static float[] anchor(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> {
            var s = mc.screen;
            float[] p = HandProjection.anchor(ItemEditor.session().pose(EditSession.MAIN), 1);
            return HandProjection.toScreen(p, s.width, s.height);
        });
    }

    private static void cursor(ClientGameTestContext ctx, float x, float y) {
        double gs = ctx.computeOnClient(mc -> (double) mc.getWindow().getWidth() / mc.screen.width);
        ctx.getInput().setCursorPos(x * gs, y * gs);
        ctx.waitTicks(1);
    }
}
