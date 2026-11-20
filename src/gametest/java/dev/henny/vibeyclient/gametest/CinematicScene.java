package dev.henny.vibeyclient.gametest;

import com.mojang.authlib.GameProfile;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.service.WaypointPoints;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.player.RemotePlayer;

import java.util.List;
import java.util.UUID;

import static dev.henny.vibeyclient.gametest.ReviewKit.cmd;

final class CinematicScene {
    static final UUID GUEST = UUID.fromString("5f1e8a0c-3c55-4d6e-9a43-2f0f9c3d7b11");

    private CinematicScene() {}

    static int[] build(ClientGameTestContext ctx, TestSingleplayerContext world) {
        int[] o = ground(ctx, world);
        int x = o[0], y = o[1], z = o[2];
        house(world, x + 7, y, z - 9);
        tower(world, x - 12, y, z - 6);
        pond(world, x - 6, y, z + 8);
        for (int i = 0; i < 6; i++) cmd(world, "setblock %d %d %d minecraft:lantern", x - 4 + i * 3, y, z - 3);
        cmd(world, "fill %d %d %d %d %d %d minecraft:oak_leaves", x + 12, y + 3, z + 6, x + 15, y + 6, z + 9);
        cmd(world, "fill %d %d %d %d %d %d minecraft:oak_log", x + 13, y, z + 7, x + 13, y + 3, z + 7);
        ctx.waitTicks(20);
        world.getClientWorld().waitForChunksRender();
        return o;
    }

    private static int[] ground(ClientGameTestContext ctx, TestSingleplayerContext world) {
        for (String c : new String[] {"gamemode creative @a", "time set 6000", "weather clear", "gamerule fall_damage false",
                "gamerule advance_time false", "gamerule spawn_mobs false", "difficulty peaceful", "gamerule send_command_feedback false"}) cmd(world, c);
        int[] p = ctx.computeOnClient(mc -> new int[] {(int) Math.floor(mc.player.getX()), (int) Math.floor(mc.player.getZ())});
        int x = p[0], z = p[1];
        int y = world.getServer().computeOnServer(s -> s.overworld().getHeight(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z));
        cmd(world, "fill %d %d %d %d %d %d minecraft:air", x - 22, y, z - 22, x + 22, y + 11, z + 22);
        cmd(world, "fill %d %d %d %d %d %d minecraft:air", x - 22, y + 12, z - 22, x + 22, y + 24, z + 22);
        cmd(world, "fill %d %d %d %d %d %d minecraft:dirt", x - 22, y - 4, z - 22, x + 22, y - 2, z + 22);
        cmd(world, "fill %d %d %d %d %d %d minecraft:grass_block", x - 22, y - 1, z - 22, x + 22, y - 1, z + 22);
        cmd(world, "tp @a %.1f %d %.1f 200 10", x + 0.5, y, z + 0.5);
        ctx.waitTicks(40);
        world.getClientWorld().waitForChunksRender();
        return new int[] {x, y, z};
    }

    private static void house(TestSingleplayerContext w, int x, int y, int z) {
        cmd(w, "fill %d %d %d %d %d %d minecraft:spruce_planks hollow", x, y, z, x + 6, y + 4, z + 5);
        cmd(w, "fill %d %d %d %d %d %d minecraft:stone_bricks", x, y - 1, z, x + 6, y - 1, z + 5);
        cmd(w, "fill %d %d %d %d %d %d minecraft:glass_pane", x + 2, y + 1, z, x + 4, y + 2, z);
        cmd(w, "fill %d %d %d %d %d %d minecraft:air", x + 3, y, z + 5, x + 3, y + 1, z + 5);
        cmd(w, "fill %d %d %d %d %d %d minecraft:dark_oak_planks", x - 1, y + 5, z - 1, x + 7, y + 5, z + 6);
        cmd(w, "setblock %d %d %d minecraft:torch", x + 3, y + 6, z + 2);
        cmd(w, "setblock %d %d %d minecraft:lantern", x + 1, y + 1, z + 6);
    }

    private static void tower(TestSingleplayerContext w, int x, int y, int z) {
        cmd(w, "fill %d %d %d %d %d %d minecraft:bricks hollow", x, y, z, x + 3, y + 9, z + 3);
        cmd(w, "fill %d %d %d %d %d %d minecraft:sea_lantern", x, y + 10, z, x + 3, y + 10, z + 3);
        cmd(w, "setblock %d %d %d minecraft:glowstone", x + 1, y + 11, z + 1);
    }

    private static void pond(TestSingleplayerContext w, int x, int y, int z) {
        cmd(w, "fill %d %d %d %d %d %d minecraft:water", x, y - 1, z, x + 6, y - 1, z + 4);
        cmd(w, "fill %d %d %d %d %d %d minecraft:sand", x - 1, y - 1, z - 1, x + 7, y - 1, z - 1);
    }

    static void guest(ClientGameTestContext ctx, int[] o) {
        ctx.runOnClient(mc -> {
            RemotePlayer p = new RemotePlayer(mc.level, new GameProfile(GUEST, "VibeyGuest"));
            p.snapTo(o[0] + 4.5, o[1], o[2] + 4.5, 140, 0);
            p.setYHeadRot(140);
            mc.level.addEntity(p);
        });
        ctx.waitTicks(5);
    }

    static void waypoint(ClientGameTestContext ctx, int[] o) {
        ctx.runOnClient(mc -> {
            String dim = mc.level.dimension().identifier().toString();
            Services.register(WaypointPoints.class, () -> List.of(new WaypointPoints.Point("Tower", dim, o[0] - 11, o[1] + 11, o[2] - 5)));
        });
    }
}
