package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.inventory.click.ClickSender;
import dev.henny.vibeyclient.module.inventory.core.ItemRef;
import dev.henny.vibeyclient.module.inventory.presets.PresetKeys;
import dev.henny.vibeyclient.module.inventory.presets.Presets;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.item.Items;

import static dev.henny.vibeyclient.gametest.InvTestKit.check;
import static dev.henny.vibeyclient.gametest.InvTestKit.cmd;
import static dev.henny.vibeyclient.gametest.InvTestKit.log;
import static dev.henny.vibeyclient.gametest.InvTestKit.shot;

final class InventoryPresetChecks {
    private InventoryPresetChecks() {}

    static void run(ClientGameTestContext ctx, TestSingleplayerContext world) {
        cmd(world, "clear @a");
        cmd(world, "item replace entity @a hotbar.0 with minecraft:diamond_sword");
        cmd(world, "item replace entity @a hotbar.1 with minecraft:bow");
        cmd(world, "item replace entity @a hotbar.2 with minecraft:cooked_beef 16");
        ctx.waitFor(mc -> mc.player.getInventory().getItem(1).is(Items.BOW), 60);
        ctx.runOnClient(mc -> {
            InvTestKit.m().presetCount.set(2);
            Presets.set().select(0);
        });

        saveWithShiftTap(ctx);
        check(Presets.set().get(0).get(0).id().equals("minecraft:diamond_sword"), "preset 1 should hold the sword");

        tap(ctx);
        check(Presets.set().active() == 1, "tap switches to preset 2");

        cmd(world, "item replace entity @a hotbar.0 with minecraft:stick");
        cmd(world, "item replace entity @a hotbar.1 with minecraft:air");
        cmd(world, "item replace entity @a inventory.5 with minecraft:diamond_sword");
        cmd(world, "item replace entity @a hotbar.4 with minecraft:bow");
        ctx.waitFor(mc -> mc.player.getInventory().getItem(4).is(Items.BOW), 60);
        saveWithShiftTap(ctx);
        check(Presets.set().get(1).get(4).equals(new ItemRef("minecraft:bow", null)), "preset 2 should hold the bow in slot 5");

        long before = ClickSender.sent();
        tap(ctx);
        ctx.waitFor(mc -> mc.player.getInventory().getItem(0).is(Items.DIAMOND_SWORD)
                && mc.player.getInventory().getItem(1).is(Items.BOW) && !ClickSender.busy(), 100);
        log("preset 1 applied with %d swap clicks", ClickSender.sent() - before);
        check(ClickSender.sent() - before == 2, "two swaps expected");
        shot(ctx, "preset_switched");

        ctx.getInput().holdKey(Keys.toNative(Keys.R));
        ctx.waitTicks(8);
        check(PresetKeys.holding(), "holding the key opens the bar");
        ctx.getInput().scroll(-1);
        ctx.waitTicks(3);
        check(PresetKeys.pending() == 1, "wheel picks preset 2, got " + PresetKeys.pending());
        shot(ctx, "preset_hold_pick");
        ctx.getInput().releaseKey(Keys.toNative(Keys.R));
        ctx.waitFor(mc -> mc.player.getInventory().getItem(0).is(Items.STICK)
                && mc.player.getInventory().getItem(4).is(Items.BOW) && !ClickSender.busy(), 100);
        check(Presets.set().active() == 1, "release applies the picked preset");
        shot(ctx, "preset_applied");
    }

    private static void saveWithShiftTap(ClientGameTestContext ctx) {
        ctx.getInput().holdShift();
        tap(ctx);
        ctx.getInput().releaseShift();
        ctx.waitTicks(2);
    }

    private static void tap(ClientGameTestContext ctx) {
        ctx.getInput().holdKey(Keys.toNative(Keys.R));
        ctx.waitTicks(2);
        ctx.getInput().releaseKey(Keys.toNative(Keys.R));
        ctx.waitTicks(3);
    }
}
