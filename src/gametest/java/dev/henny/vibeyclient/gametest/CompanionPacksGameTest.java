package dev.henny.vibeyclient.gametest;

import dev.henny.companio.mc.CompanioMc;
import dev.henny.vui.mc.host.VuiScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.level.levelgen.Heightmap;

import static dev.henny.vibeyclient.gametest.CompanionTestKit.*;

public final class CompanionPacksGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        ctx.runOnClient(mc -> {
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
        });
        try (TestSingleplayerContext world = ctx.worldBuilder().setUseConsistentSettings(false)
                .adjustSettings(s -> s.setSeed("vibey-companion-packs")).create()) {
            world.getClientWorld().waitForChunksRender();
            int[] o = scene(ctx, world);
            PenguinScenes.run(ctx, world, o);
            RobotScenes.run(ctx, world, o);
            packScreen(ctx);
        }
    }

    private static int[] scene(ClientGameTestContext ctx, TestSingleplayerContext world) {
        for (String c : new String[] {"gamemode creative @a", "time set 6000", "weather clear", "gamerule advance_time false",
                "gamerule advance_weather false", "gamerule spawn_mobs false", "gamerule send_command_feedback false", "difficulty peaceful"}) cmd(world, c);
        int[] p = ctx.computeOnClient(mc -> {
            int x = (int) Math.floor(mc.player.getX()), z = (int) Math.floor(mc.player.getZ());
            return new int[] {x, mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z};
        });
        int x = p[0], y = Math.max(p[1], 64), z = p[2];
        cmd(world, "fill %d %d %d %d %d %d minecraft:air", x - 24, y, z - 24, x + 24, y + 6, z + 24);
        cmd(world, "fill %d %d %d %d %d %d minecraft:air", x - 24, y + 7, z - 24, x + 24, y + 16, z + 24);
        cmd(world, "fill %d %d %d %d %d %d minecraft:grass_block", x - 24, y - 3, z - 24, x + 24, y - 1, z + 24);
        cmd(world, "fillbiome %d %d %d %d %d %d minecraft:plains", x - 24, y - 3, z - 24, x + 24, y + 7, z + 24);
        cmd(world, "fill %d %d %d %d %d %d minecraft:water", x + 5, y - 2, z - 3, x + 10, y - 1, z + 3);
        cmd(world, "fill %d %d %d %d %d %d minecraft:snow_block", x - 15, y - 1, z - 4, x - 5, y - 1, z + 4);
        cmd(world, "fill %d %d %d %d %d %d minecraft:packed_ice", x - 15, y - 1, z + 6, x - 5, y - 1, z + 10);
        cmd(world, "fillbiome %d %d %d %d %d %d minecraft:snowy_plains", x - 16, y - 4, z - 5, x - 6, y + 14, z + 11);
        cmd(world, "fill %d %d %d %d %d %d minecraft:sand", x + 11, y - 1, z - 23, x + 23, y - 1, z - 9);
        cmd(world, "fillbiome %d %d %d %d %d %d minecraft:desert", x + 11, y - 4, z - 23, x + 23, y + 14, z - 9);
        cmd(world, "fill %d %d %d %d %d %d minecraft:stone", x - 4, y, z - 16, x + 4, y + 2, z - 16);
        cmd(world, "setblock %d %d %d minecraft:diamond_ore", x, y + 1, z - 16);
        cmd(world, "setblock %d %d %d minecraft:iron_ore", x - 2, y + 1, z - 16);
        cmd(world, "setblock %d %d %d minecraft:gold_ore", x + 2, y, z - 16);
        cmd(world, "setworldspawn %d %d %d", x, y, z);
        tp(world, ctx, x + 0.5, y, z + 0.5, 180, 10);
        ctx.waitTicks(20);
        world.getClientWorld().waitForChunksRender();
        log("packs scene at %d %d %d", x, y, z);
        return new int[] {x, y, z};
    }

    private static void packScreen(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> CompanioMc.openPacks());
        ctx.waitTicks(10);
        String errors = ctx.computeOnClient(mc -> ((VuiScreen) mc.screen).document().diagnostics().format());
        check(errors.isBlank(), "pack screen errors: " + errors);
        long packs = ctx.computeOnClient(mc -> CompanioMc.packs().all().stream()
                .filter(p -> p.errors() == 0 && java.util.List.of("seal", "penguin", "robot").contains(p.id())).count());
        check(packs == 3, "seal, penguin and robot load without errors, got " + packs);
        PackShots.snap(ctx, "pack_screen");
        ctx.getInput().pressKey(ESCAPE);
        ctx.waitTicks(4);
    }
}
