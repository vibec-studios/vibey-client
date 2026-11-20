package dev.henny.vibeyclient.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import static dev.henny.vibeyclient.gametest.ReviewKit.cmd;

final class CameraScene {
    private static final String[] PILLARS = {"red_concrete", "yellow_concrete", "lime_concrete", "light_blue_concrete",
            "purple_concrete", "orange_concrete", "white_concrete", "black_concrete"};

    private CameraScene() {}

    static int[] build(ClientGameTestContext ctx, TestSingleplayerContext world) {
        int[] o = ReviewKit.platform(ctx, world, "smooth_stone");
        int x = o[0], y = o[1], z = o[2];
        cmd(world, "gamerule send_command_feedback false");
        for (int i = 0; i < PILLARS.length; i++) {
            double a = Math.PI * 2 * i / PILLARS.length;
            int px = x + (int) Math.round(Math.sin(a) * 6), pz = z - (int) Math.round(Math.cos(a) * 6);
            cmd(world, "fill %d %d %d %d %d %d minecraft:%s", px, y, pz, px, y + 3 + i % 3, pz, PILLARS[i]);
        }
        cmd(world, "setblock %d %d %d minecraft:diamond_block", x, y, z - 2);
        cmd(world, "setblock %d %d %d minecraft:gold_block", x - 2, y, z + 2);
        ctx.waitTicks(10);
        world.getClientWorld().waitForChunksRender();
        return o;
    }
}
