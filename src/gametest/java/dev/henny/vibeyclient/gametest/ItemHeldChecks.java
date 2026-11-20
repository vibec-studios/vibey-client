package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.module.visuals.items.core.HandPreset;
import dev.henny.vibeyclient.module.visuals.items.core.SwingStyle;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.InteractionHand;

import static dev.henny.vibeyclient.gametest.ItemModelsGameTest.KIT;
import static dev.henny.vibeyclient.gametest.ItemModelsGameTest.m;
import static dev.henny.vibeyclient.gametest.ReviewKit.check;
import static dev.henny.vibeyclient.gametest.ReviewKit.cmd;

final class ItemHeldChecks {
    static final String SWORD = "minecraft:diamond_sword[minecraft:enchantments={\"minecraft:sharpness\":3}]";

    private ItemHeldChecks() {}

    static void hold(ClientGameTestContext ctx, TestSingleplayerContext world, String main, String off) {
        cmd(world, "item replace entity @a weapon.mainhand with " + main);
        cmd(world, "item replace entity @a weapon.offhand with " + off);
        ctx.waitTicks(20);
    }

    static void poses(ClientGameTestContext ctx, TestSingleplayerContext world) {
        hold(ctx, world, SWORD, "minecraft:torch");
        ReviewKit.look(ctx, 180, 0);
        KIT.shot(ctx, "pose_vanilla");
        for (HandPreset p : new HandPreset[] {HandPreset.SMALL, HandPreset.LOW, HandPreset.CENTERED}) {
            ctx.runOnClient(mc -> m().held.preset.set(p.id()));
            ctx.waitTicks(3);
            String id = ctx.computeOnClient(mc -> m().held.preset.get());
            check(p.id().equals(id), "preset " + p.id() + " should match, got " + id);
            KIT.shot(ctx, "pose_" + p.id());
        }
        ctx.runOnClient(mc -> {
            m().held.main.set(HandPreset.VANILLA.pose().moved(-0.1f, 0.05f, -0.1f).rotated(-20, 25, 15).scaled(1.3f));
            m().swing.hideEmptyHand.set(true);
        });
        ctx.waitTicks(3);
        KIT.shot(ctx, "pose_custom_turned");
        ItemModelsGameTest.reset(ctx);
    }

    static void swing(ClientGameTestContext ctx) {
        int vanilla = swingTicks(ctx);
        ctx.runOnClient(mc -> m().swing.speed.set(50));
        int slow = swingTicks(ctx);
        ctx.runOnClient(mc -> m().swing.speed.set(200));
        int fast = swingTicks(ctx);
        KIT.log("swing ticks: vanilla %d, 50 %% %d, 200 %% %d", vanilla, slow, fast);
        check(slow > vanilla && fast < vanilla, "swing speed should change the swing length");
        ctx.runOnClient(mc -> {
            m().swing.speed.set(60);
            m().swing.style.set(SwingStyle.PUNCH);
            mc.player.swing(InteractionHand.MAIN_HAND);
        });
        ctx.waitTicks(3);
        KIT.shotNow(ctx, "swing_punch");
        ItemModelsGameTest.reset(ctx);
    }

    private static int swingTicks(ClientGameTestContext ctx) {
        ctx.waitTicks(20);
        ctx.runOnClient(mc -> mc.player.swing(InteractionHand.MAIN_HAND));
        for (int t = 1; t < 60; t++) {
            ctx.waitTicks(1);
            if (!ctx.computeOnClient(mc -> mc.player.swinging)) return t;
        }
        return 60;
    }
}
