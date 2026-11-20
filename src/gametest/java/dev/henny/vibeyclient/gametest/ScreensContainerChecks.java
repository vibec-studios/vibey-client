package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.screens.apply.Item;
import dev.henny.vibeyclient.module.screens.mixin.ContainerAccess;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.Slot;

import static dev.henny.vibeyclient.gametest.ScreensTestKit.*;

final class ScreensContainerChecks {
    private ScreensContainerChecks() {}

    static void inventory(ClientGameTestContext ctx, TestSingleplayerContext world) {
        ReviewKit.cmd(world, "clear @a");
        ReviewKit.cmd(world, "give @a minecraft:diamond 5");
        ctx.waitTicks(5);
        ctx.setScreen(() -> new InventoryScreen(net.minecraft.client.Minecraft.getInstance().player));
        ctx.waitTicks(5);
        log("inventory widgets: %s", ids(ctx));
        movePanel(ctx, 70, 12);
        shot(ctx, "inventory_moved");
        pickAndReturn(ctx, 36, "diamond");
        ctx.getInput().pressKey(Keys.F9);
        ctx.waitTicks(3);
        float[] p = box(ctx, Item.PANEL);
        hover(ctx, p[0] + 20, p[1] + 10);
        ctx.getInput().scroll(1);
        ctx.waitTicks(5);
        float k = layout(ctx).scale();
        check(k == 1.1f, "wheel on the panel did not scale the screen: " + k);
        hover(ctx, 3, 3);
        shot(ctx, "inventory_scaled_edit");
        ctx.getInput().pressKey(Keys.ESCAPE);
        ctx.waitTicks(3);
        pickAndReturn(ctx, 36, "diamond");
        shot(ctx, "inventory_scaled");
        ctx.runOnClient(mc -> mc.player.closeContainer());
        ctx.setScreen(() -> null);
    }

    static void chest(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        ReviewKit.cmd(world, "clear @a");
        ReviewKit.cmd(world, "setblock %d %d %d minecraft:chest{Items:[{Slot:0b,id:\"minecraft:cobblestone\",count:32},"
                + "{Slot:4b,id:\"minecraft:gold_ingot\",count:7}]}", o[0] + 2, o[1], o[2]);
        ctx.waitTicks(5);
        BlockPos pos = new BlockPos(o[0] + 2, o[1], o[2]);
        world.getServer().runOnServer(s -> s.getPlayerList().getPlayers().get(0).openMenu((MenuProvider) s.overworld().getBlockEntity(pos)));
        ctx.waitFor(mc -> mc.screen instanceof ContainerScreen, 100);
        ctx.waitTicks(5);
        movePanel(ctx, -90, -20);
        shot(ctx, "chest_moved");
        pickAndReturn(ctx, 4, "gold_ingot");
        ctx.runOnClient(mc -> mc.player.closeContainer());
        ctx.setScreen(() -> null);
    }

    private static void movePanel(ClientGameTestContext ctx, int dx, int dy) {
        ctx.getInput().pressKey(Keys.F9);
        ctx.waitTicks(3);
        check(editing(ctx), "F9 did not start edit mode in a container");
        float[] p = box(ctx, Item.PANEL);
        check(p != null, "no panel item");
        int[] before = offset(ctx);
        dragFixed(ctx, p[0] + p[2] - 6, p[1] + 4, dx, dy);
        int[] after = offset(ctx);
        log("panel %d,%d -> %d,%d", before[0], before[1], after[0], after[1]);
        check(Math.abs(after[0] - before[0] - dx) <= 5 && Math.abs(after[1] - before[1] - dy) <= 5,
                "panel did not move by " + dx + "," + dy);
        hover(ctx, p[0] + p[2] - 6, p[1] + 4);
        shot(ctx, "panel_edit");
        ctx.getInput().pressKey(Keys.ESCAPE);
        ctx.waitTicks(3);
        check(!editing(ctx), "Esc did not leave edit mode");
        check(ctx.computeOnClient(mc -> mc.screen instanceof AbstractContainerScreen<?>), "Esc in edit mode closed the container");
    }

    private static int[] offset(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> new int[] {Math.round(session(mc).offsetX()), Math.round(session(mc).offsetY())});
    }

    private static void pickAndReturn(ClientGameTestContext ctx, int slot, String item) {
        double[] at = ctx.computeOnClient(mc -> {
            AbstractContainerScreen<?> s = (AbstractContainerScreen<?>) mc.screen;
            ContainerAccess a = (ContainerAccess) s;
            Slot sl = s.getMenu().getSlot(slot);
            return new double[] {a.vibey$leftPos() + sl.x + 8, a.vibey$topPos() + sl.y + 8};
        });
        click(ctx, at[0], at[1], 0);
        String carried = ctx.computeOnClient(mc -> mc.player.containerMenu.getCarried().getItem().toString());
        log("carried after click on slot %d: %s", slot, carried);
        check(carried.contains(item), "slot click at the moved panel picked up '" + carried + "', wanted " + item);
        click(ctx, at[0], at[1], 0);
        String back = ctx.computeOnClient(mc -> mc.player.containerMenu.getSlot(slot).getItem().getItem().toString());
        check(back.contains(item), "item not put back: " + back);
    }
}
