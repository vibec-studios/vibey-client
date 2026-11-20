package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.module.inventory.tooltip.ContainerPreview;
import dev.henny.vibeyclient.module.inventory.tooltip.PreviewItems;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static dev.henny.vibeyclient.gametest.InvTestKit.check;
import static dev.henny.vibeyclient.gametest.InvTestKit.cmd;
import static dev.henny.vibeyclient.gametest.InvTestKit.hoverSlot;
import static dev.henny.vibeyclient.gametest.InvTestKit.log;
import static dev.henny.vibeyclient.gametest.InvTestKit.m;
import static dev.henny.vibeyclient.gametest.InvTestKit.shot;

final class InventoryTooltipChecks {
    static final int HOTBAR_0 = 36;

    private InventoryTooltipChecks() {}

    static void run(ClientGameTestContext ctx, TestSingleplayerContext world) {
        cmd(world, "clear @a");
        cmd(world, "give @a minecraft:shulker_box[minecraft:container=[{slot:0,item:{id:\"minecraft:diamond\",count:5}},"
                + "{slot:4,item:{id:\"minecraft:torch\",count:64}},{slot:13,item:{id:\"minecraft:golden_apple\",count:3}},"
                + "{slot:26,item:{id:\"minecraft:ender_pearl\",count:16}}]]");
        cmd(world, "give @a minecraft:diamond_pickaxe[minecraft:damage=500]");
        ctx.waitFor(mc -> !mc.player.getInventory().getItem(1).isEmpty(), 100);
        ctx.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
        ctx.waitTicks(5);

        ContainerPreview preview = ctx.computeOnClient(mc -> PreviewItems.of(mc.player.getInventory().getItem(0)));
        check(preview != null && preview.items().size() == 27, "shulker preview should have 27 cells");
        List<String> boxLines = lines(ctx, 0);
        check(boxLines.stream().noneMatch(l -> l.contains("x5")), "vanilla content list should be replaced: " + boxLines);
        hoverSlot(ctx, HOTBAR_0);
        ctx.waitTicks(4);
        shot(ctx, "shulker_preview");

        List<String> pickLines = lines(ctx, 1);
        log("pickaxe tooltip %s", pickLines);
        check(pickLines.stream().anyMatch(l -> l.contains("67%") && l.contains("1061")), "durability line missing: " + pickLines);
        hoverSlot(ctx, HOTBAR_0 + 1);
        ctx.waitTicks(4);
        shot(ctx, "durability");

        ctx.runOnClient(mc -> m().preview.set(false));
        check(lines(ctx, 0).stream().anyMatch(l -> l.contains("x5")), "vanilla list should come back with the preview off");
        ctx.runOnClient(mc -> m().preview.set(true));
        ctx.setScreen(() -> null);
    }

    private static List<String> lines(ClientGameTestContext ctx, int invSlot) {
        return ctx.computeOnClient(mc -> {
            ItemStack st = mc.player.getInventory().getItem(invSlot);
            return Screen.getTooltipFromItem(mc, st).stream().map(Component::getString).toList();
        });
    }
}
