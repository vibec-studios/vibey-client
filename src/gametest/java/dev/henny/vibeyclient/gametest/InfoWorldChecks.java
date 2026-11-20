package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.module.info.core.SpawnLight;
import dev.henny.vibeyclient.module.info.light.ChunkBorders;
import dev.henny.vibeyclient.module.info.light.LightLabel;
import dev.henny.vibeyclient.module.info.light.LightProbe;
import dev.henny.vibeyclient.module.info.tnt.TntTimers;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import org.joml.Vector3d;

import static dev.henny.vibeyclient.gametest.InfoTestKit.check;
import static dev.henny.vibeyclient.gametest.InfoTestKit.cmd;
import static dev.henny.vibeyclient.gametest.InfoTestKit.log;
import static dev.henny.vibeyclient.gametest.InfoTestKit.lookAt;
import static dev.henny.vibeyclient.gametest.InfoTestKit.m;
import static dev.henny.vibeyclient.gametest.InfoTestKit.shot;

final class InfoWorldChecks {
    private InfoWorldChecks() {}

    static void light(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        int x = o[0], y = o[1], z = o[2];
        cmd(world, "setblock %d %d %d minecraft:torch", x - 3, y, z);
        cmd(world, "item replace entity @a weapon.mainhand with minecraft:torch");
        ctx.waitTicks(10);
        Vector3d floor = new Vector3d(x - 1.5, y - 0.02, z + 0.5);
        lookAt(ctx, floor);
        BlockPos above = new BlockPos(x - 3 + 1, y, z);
        int expected = ctx.computeOnClient(mc -> mc.level.getBrightness(LightLayer.BLOCK, above));
        check(ctx.computeOnClient(mc -> LightLabel.visible()), "light label should show while holding a torch");
        String shown = ctx.computeOnClient(mc -> LightLabel.value());
        log("light near torch: shown %s, expected %d", shown, expected);
        check(shown.equals(Integer.toString(expected)), "light label " + shown + " != " + expected);
        check(ctx.computeOnClient(mc -> LightProbe.color()) == SpawnLight.SAFE.color(), "lit floor should be safe (teal)");
        shot(ctx, "light_near_torch");

        cmd(world, "setblock %d %d %d minecraft:air", x - 3, y, z);
        ctx.waitTicks(10);
        lookAt(ctx, floor);
        check("0".equals(ctx.computeOnClient(mc -> LightLabel.value())), "dark floor should read 0");
        check(ctx.computeOnClient(mc -> LightProbe.color()) == SpawnLight.NIGHT.color(), "open dark floor: spawns at night");
        shot(ctx, "light_dark_floor_night");

        cmd(world, "item replace entity @a weapon.mainhand with minecraft:air");
        ctx.waitTicks(4);
        check(!ctx.computeOnClient(mc -> LightLabel.visible()), "no torch in hand: no light label");
        ctx.runOnClient(mc -> m().lightTorch.set(false));
        ctx.waitTicks(3);
        check(ctx.computeOnClient(mc -> LightLabel.visible()), "lightTorch off: label always");

        cmd(world, "gamerule reduced_debug_info true");
        ctx.waitTicks(6);
        boolean reduced = ctx.computeOnClient(mc -> mc.showOnlyReducedInfo());
        if (reduced) check(!ctx.computeOnClient(mc -> LightLabel.visible()), "reduced debug info must hide the light label");
        else log("reducedDebugInfo gamerule not applied, skipped");
        shot(ctx, "light_reduced_debug_hidden");
        cmd(world, "gamerule reduced_debug_info false");
        ctx.runOnClient(mc -> m().lightTorch.reset());
        ctx.waitTicks(6);
    }

    static void chunks(ClientGameTestContext ctx, int[] o) {
        lookAt(ctx, new Vector3d(o[0] + 6.5, o[1] + 1.5, o[2] - 12));
        boolean before = ctx.computeOnClient(mc -> ChunkBorders.shown());
        ctx.getInput().pressKey(Keys.O);
        ctx.waitTicks(4);
        check(ctx.computeOnClient(mc -> ChunkBorders.shown()) != before, "chunk key should toggle the borders");
        shot(ctx, "chunk_borders_on");
        ctx.getInput().pressKey(Keys.O);
        ctx.waitTicks(4);
        check(ctx.computeOnClient(mc -> ChunkBorders.shown()) == before, "second press should toggle back");
    }

    static void tnt(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        int x = o[0], y = o[1], z = o[2];
        cmd(world, "summon minecraft:tnt %.1f %d %.1f {fuse:80}", x + 0.5, y, z - 4.5);
        ctx.waitTicks(3);
        lookAt(ctx, new Vector3d(x + 0.5, y + 0.5, z - 4.5));
        ctx.waitTicks(2);
        check(ctx.computeOnClient(mc -> TntTimers.count()) == 1, "looking at lit TNT should start one timer");
        float first = Float.parseFloat(ctx.computeOnClient(mc -> TntTimers.firstLabel()));
        shot(ctx, "tnt_timer_first");
        lookAt(ctx, new Vector3d(x + 3.5, y + 1, z - 4.5));
        ctx.waitTicks(20);
        float later = Float.parseFloat(ctx.computeOnClient(mc -> TntTimers.firstLabel()));
        log("tnt fuse %.1f -> %.1f", first, later);
        check(first > 2.5f && first <= 4f, "fresh TNT should show about 4 s, was " + first);
        check(later < first - 0.8f, "timer should count down and stay after looking away");
        shot(ctx, "tnt_timer_later");
        cmd(world, "kill @e[type=minecraft:tnt]");
        ctx.waitTicks(4);
        check(ctx.computeOnClient(mc -> TntTimers.count()) == 0, "timer should go away with the TNT");
    }
}
