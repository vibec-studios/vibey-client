package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.inventory.mixin.ContainerScreenAccess;
import dev.henny.vibeyclient.module.inventory.screen.ContainerSessions;
import dev.henny.vibeyclient.module.inventory.InventoryRules;
import dev.henny.vibeyclient.module.inventory.click.ClickSender;
import dev.henny.vibeyclient.module.inventory.search.SearchState;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.Items;

import java.util.List;

import static dev.henny.vibeyclient.gametest.InvTestKit.check;
import static dev.henny.vibeyclient.gametest.InvTestKit.cmd;
import static dev.henny.vibeyclient.gametest.InvTestKit.count;
import static dev.henny.vibeyclient.gametest.InvTestKit.hoverSlot;
import static dev.henny.vibeyclient.gametest.InvTestKit.log;
import static dev.henny.vibeyclient.gametest.InvTestKit.m;
import static dev.henny.vibeyclient.gametest.InvTestKit.shot;

final class InventoryTweakChecks {
    static final int COBBLE = 0, DIRT = 1, SAND = 2, GRAVEL = 3, DIAMOND = 9;

    private InventoryTweakChecks() {}

    static void run(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        openChest(ctx, world, o);
        shot(ctx, "chest_before");
        wheel(ctx);
        rate(ctx);
        drag(ctx);
        shot(ctx, "chest_after_moves");
        search(ctx);
        ctx.runOnClient(mc -> mc.player.closeContainer());
        ctx.waitTicks(2);
    }

    static void openChest(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        cmd(world, "clear @a");
        cmd(world, "setblock %d %d %d minecraft:air", o[0] + 2, o[1], o[2]);
        cmd(world, "setblock %d %d %d minecraft:chest{Items:[{Slot:0b,id:\"minecraft:cobblestone\",count:32},"
                + "{Slot:1b,id:\"minecraft:dirt\",count:16},{Slot:2b,id:\"minecraft:sand\",count:16},"
                + "{Slot:3b,id:\"minecraft:gravel\",count:16},{Slot:9b,id:\"minecraft:diamond\",count:3}]}", o[0] + 2, o[1], o[2]);
        ctx.waitTicks(5);
        BlockPos pos = new BlockPos(o[0] + 2, o[1], o[2]);
        world.getServer().runOnServer(s -> s.getPlayerList().getPlayers().get(0).openMenu((MenuProvider) s.overworld().getBlockEntity(pos)));
        ctx.waitFor(mc -> mc.screen instanceof ContainerScreen, 100);
        ctx.waitTicks(5);
    }

    private static void wheel(ClientGameTestContext ctx) {
        long before = ClickSender.sent();
        hoverSlot(ctx, COBBLE);
        ctx.getInput().scroll(-1);
        ctx.waitFor(mc -> mc.player.getInventory().countItem(Items.COBBLESTONE) == 1 && !ClickSender.busy(), 60);
        check(count(ctx, COBBLE) == 31, "wheel down should move one cobblestone");
        check(ClickSender.sent() - before == 3, "one item = 3 clicks, got " + (ClickSender.sent() - before));
        ctx.getInput().scroll(1);
        ctx.waitFor(mc -> mc.player.containerMenu.getSlot(COBBLE).getItem().getCount() == 32 && !ClickSender.busy(), 60);
        ctx.getInput().holdShift();
        ctx.getInput().scroll(-1);
        ctx.getInput().releaseShift();
        ctx.waitFor(mc -> mc.player.containerMenu.getSlot(COBBLE).getItem().isEmpty() && !ClickSender.busy(), 60);
        check(ctx.computeOnClient(mc -> mc.player.getInventory().countItem(Items.COBBLESTONE)) == 32, "shift+wheel moves the stack");
    }

    private static void rate(ClientGameTestContext ctx) {
        hoverSlot(ctx, DIRT);
        long before = ClickSender.sent();
        long start = System.nanoTime();
        for (int i = 0; i < 6; i++) ctx.getInput().scroll(-1);
        ctx.waitFor(mc -> !ClickSender.busy(), 200);
        long clicks = ClickSender.sent() - before;
        double seconds = (System.nanoTime() - start) / 1e9;
        log("6 wheel notches -> %d clicks in %.2f s (limit %d/s)", clicks, seconds, m().clickRate.get());
        check(clicks == 9, "only 3 wheel moves may queue (9 clicks), got " + clicks);
        check(seconds >= (clicks - 1) / (double) m().clickRate.get() - 0.02, "clicks faster than the limit");
        check(count(ctx, DIRT) == 13, "three dirt moved");
    }

    private static void drag(ClientGameTestContext ctx) {
        hoverSlot(ctx, SAND);
        ctx.getInput().holdShift();
        ctx.getInput().holdMouse(0);
        ctx.waitTicks(2);
        log("drag after press: %s, shift %s", dragState(ctx), ctx.computeOnClient(mc -> ClientCompat.shiftDown()));
        for (int slot : List.of(GRAVEL, 4, 5)) {
            hoverSlot(ctx, slot);
            log("drag over %d: %s", slot, dragState(ctx));
        }
        ctx.getInput().releaseMouse(0);
        ctx.getInput().releaseShift();
        ctx.waitFor(mc -> !ClickSender.busy(), 100);
        check(count(ctx, SAND) == 0 && count(ctx, GRAVEL) == 0, "shift-drag should move sand and gravel, left " + count(ctx, SAND) + "/" + count(ctx, GRAVEL));
        check(count(ctx, DIRT) == 13 && count(ctx, DIAMOND) == 3, "drag must not touch other slots");
    }

    private static String dragState(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> {
            var s = ContainerSessions.current();
            var h = ((ContainerScreenAccess) mc.screen).vibey$hoveredSlot();
            return s == null ? "no session" : "dragging=" + s.dragging() + " busy=" + ClickSender.busy() + " sent=" + ClickSender.sent()
                    + " hovered=" + (h == null ? -1 : h.index) + " carried=" + mc.player.containerMenu.getCarried() + " shift=" + ClientCompat.shiftDown();
        });
    }

    private static void search(ClientGameTestContext ctx) {
        ctx.getInput().holdControl();
        ctx.getInput().pressKey(Keys.toNative(Keys.F));
        ctx.getInput().releaseControl();
        ctx.waitTicks(2);
        ctx.getInput().typeChars("diamond");
        ctx.waitTicks(10);
        check(ctx.computeOnClient(mc -> mc.screen instanceof ContainerScreen), "typing must not close the chest");
        check(SearchState.count() == 1 && SearchState.hit(DIAMOND) && !SearchState.hit(DIRT),
                "search should find only the diamonds, count " + SearchState.count());
        hoverSlot(ctx, 20);
        shot(ctx, "search_highlight");
        ctx.getInput().pressKey(Keys.toNative(Keys.ESCAPE));
        ctx.waitTicks(2);
        check(ctx.computeOnClient(mc -> mc.screen instanceof ContainerScreen), "first Esc only leaves the field");
    }

    static void rule(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        openChest(ctx, world, o);
        ctx.runOnClient(mc -> ServerRules.apply(List.of(InventoryRules.TWEAKS), "No quick moves here"));
        hoverSlot(ctx, COBBLE);
        long before = ClickSender.sent();
        ctx.getInput().scroll(-1);
        ctx.waitTicks(10);
        check(ClickSender.sent() == before && count(ctx, COBBLE) == 32, "server rule must stop quick moves");
        ctx.runOnClient(mc -> ServerRules.clear());
        ctx.runOnClient(mc -> mc.player.closeContainer());
        ctx.waitTicks(2);
    }
}
