package dev.henny.vibeyclient.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import org.joml.Vector3d;

import static dev.henny.vibeyclient.gametest.ItemModelsGameTest.KIT;
import static dev.henny.vibeyclient.gametest.ItemModelsGameTest.m;
import static dev.henny.vibeyclient.gametest.ReviewKit.cmd;

final class ItemWorldChecks {
    private static final String[] DROPS = {"diamond", "golden_apple", "iron_sword", "oak_log", "emerald"};

    private ItemWorldChecks() {}

    static void glint(ClientGameTestContext ctx, TestSingleplayerContext world) {
        ItemHeldChecks.hold(ctx, world, ItemHeldChecks.SWORD, "minecraft:enchanted_book[minecraft:stored_enchantments={\"minecraft:mending\":1}]");
        ReviewKit.look(ctx, 180, 0);
        ctx.runOnClient(mc -> m().held.main.set(m().held.main.pose().moved(-0.12f, 0.08f, 0.1f).scaled(1.4f)));
        ctx.waitTicks(3);
        KIT.shot(ctx, "glint_vanilla");
        ctx.runOnClient(mc -> {
            m().glint.tint.set(true);
            m().glint.color.set(0xFFFF4D6D);
            m().glint.strength.set(160);
        });
        ctx.waitTicks(3);
        KIT.shot(ctx, "glint_red");
        ctx.runOnClient(mc -> m().glint.color.set(0xFF3DDC84));
        ctx.waitTicks(3);
        KIT.shot(ctx, "glint_green");
        ctx.runOnClient(mc -> m().glint.strength.set(0));
        ctx.waitTicks(3);
        KIT.shot(ctx, "glint_off");
        ItemModelsGameTest.reset(ctx);
    }

    static void drops(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        ItemHeldChecks.hold(ctx, world, "minecraft:air", "minecraft:air");
        ReviewKit.lookAt(ctx, new Vector3d(o[0] + 0.5, o[1] + 0.2, o[2] - 3.5));
        drop(world, o);
        ctx.waitTicks(60);
        KIT.shot(ctx, "drops_vanilla_size");
        ctx.runOnClient(mc -> m().drops.dropScale.set(300));
        ctx.waitTicks(3);
        KIT.shot(ctx, "drops_big_physics");
        ctx.runOnClient(mc -> m().itemPhysics.set(false));
        ctx.waitTicks(3);
        KIT.shot(ctx, "drops_big_vanilla");
        cmd(world, "kill @e[type=item]");
        ItemModelsGameTest.reset(ctx);
    }

    static void frames(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        int x = o[0], y = o[1], z = o[2] - 3;
        cmd(world, "fill %d %d %d %d %d %d minecraft:stone_bricks", x - 1, y, z, x + 1, y + 2, z);
        cmd(world, "summon minecraft:item_frame %d %d %d {Facing:3b,Item:{id:\"minecraft:diamond_sword\",count:1}}", x - 1, y + 1, z + 1);
        cmd(world, "summon minecraft:glow_item_frame %d %d %d {Facing:3b,Item:{id:\"minecraft:golden_apple\",count:1}}", x + 1, y + 1, z + 1);
        ctx.waitTicks(10);
        ReviewKit.lookAt(ctx, new Vector3d(x + 0.5, y + 1.5, z + 0.9));
        KIT.shot(ctx, "frames_vanilla");
        ctx.runOnClient(mc -> m().drops.frameScale.set(180));
        ctx.waitTicks(3);
        KIT.shot(ctx, "frames_big");
        cmd(world, "kill @e[type=item_frame]");
        cmd(world, "kill @e[type=glow_item_frame]");
        cmd(world, "fill %d %d %d %d %d %d minecraft:air", x - 1, y, z, x + 1, y + 2, z);
        ItemModelsGameTest.reset(ctx);
    }

    private static void drop(TestSingleplayerContext world, int[] o) {
        for (int i = 0; i < DROPS.length; i++) {
            cmd(world, "summon minecraft:item %.2f %d %.2f {Item:{id:\"minecraft:%s\",count:1},PickupDelay:32767}",
                    o[0] + 0.5 + (i - 2) * 1.1, o[1] + 1, o[2] - 3.5, DROPS[i]);
        }
    }
}
