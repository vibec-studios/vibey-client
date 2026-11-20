package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.module.visuals.effects.screen.EffectsScreen;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.world.phys.HitResult;
import org.joml.Vector3d;

import static dev.henny.vibeyclient.gametest.EffectsGameTest.KIT;
import static dev.henny.vibeyclient.gametest.EffectsGameTest.pvp;
import static dev.henny.vibeyclient.gametest.ReviewKit.check;
import static dev.henny.vibeyclient.gametest.ReviewKit.cmd;

final class EffectsWorldChecks {
    private EffectsWorldChecks() {}

    static void world(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        outline(ctx, o);
        hit(ctx, world, o);
    }

    private static void outline(ClientGameTestContext ctx, int[] o) {
        ReviewKit.lookAt(ctx, new Vector3d(o[0] + 0.5, o[1] + 0.5, o[2] - 2.5));
        check(ctx.computeOnClient(mc -> mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK), "not looking at the block");
        KIT.shot(ctx, "outline_vanilla");
        ctx.runOnClient(mc -> {
            pvp().outline.set(true);
            pvp().outlineColor.set(0xFF3BFF6B);
            pvp().outlineWidth.set(5f);
            pvp().outlineFill.set(true);
            pvp().outlineFillOpacity.set(25);
        });
        ctx.waitTicks(3);
        KIT.shot(ctx, "outline_lime_fill");
        EffectsEditorChecks.open(ctx);
        ctx.runOnClient(mc -> pvp().outlineColor.set(0xFFFF5CE1));
        ctx.waitTicks(8);
        check(ctx.computeOnClient(mc -> mc.screen instanceof EffectsScreen), "editor closed");
        KIT.shot(ctx, "outline_pink_live_in_editor");
        ctx.setScreen(() -> null);
        ctx.runOnClient(mc -> EffectsGameTest.resetAll());
    }

    private static void hit(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        cmd(world, "summon minecraft:iron_golem %.1f %d %.1f {NoAI:1b,Rotation:[0f,0f],Health:100f}", o[0] + 0.5, o[1], o[2] - 5.5);
        ctx.waitTicks(10);
        ReviewKit.look(ctx, 180, 4);
        hurt(ctx, world, "hit_vanilla_red", false);
        hurt(ctx, world, "hit_blue", true);
        cmd(world, "kill @e[type=minecraft:iron_golem]");
        ctx.waitTicks(30);
        cmd(world, "kill @e[type=minecraft:item]");
        ctx.waitTicks(30);
        ctx.runOnClient(mc -> EffectsGameTest.resetAll());
    }

    private static void hurt(ClientGameTestContext ctx, TestSingleplayerContext world, String shot, boolean blue) {
        ctx.runOnClient(mc -> {
            pvp().hitColor.set(blue);
            pvp().hitTint.set(0xFF4D7BFF);
            pvp().hitStrength.set(45);
        });
        ctx.waitTicks(12);
        cmd(world, "damage @e[type=minecraft:iron_golem,limit=1] 1");
        ctx.waitTicks(2);
        KIT.shotNow(ctx, shot);
    }
}
