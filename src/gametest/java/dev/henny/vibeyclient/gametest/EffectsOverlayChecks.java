package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.module.visuals.effects.Effects;
import dev.henny.vibeyclient.module.visuals.effects.EffectsPreview;
import dev.henny.vibeyclient.module.visuals.effects.ExplosionShake;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import static dev.henny.vibeyclient.gametest.EffectsGameTest.KIT;
import static dev.henny.vibeyclient.gametest.EffectsGameTest.fx;
import static dev.henny.vibeyclient.gametest.EffectsGameTest.pvp;
import static dev.henny.vibeyclient.gametest.ReviewKit.check;
import static dev.henny.vibeyclient.gametest.ReviewKit.cmd;

final class EffectsOverlayChecks {
    private EffectsOverlayChecks() {}

    static void overlays(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        fire(ctx, world);
        pumpkin(ctx, world);
        totem(ctx);
        blast(ctx);
        hurt(ctx);
    }

    private static void fire(ClientGameTestContext ctx, TestSingleplayerContext world) {
        cmd(world, "gamemode survival @a");
        cmd(world, "effect give @a minecraft:fire_resistance infinite 0 true");
        cmd(world, "effect give @a minecraft:resistance infinite 4 true");
        ReviewKit.look(ctx, 180, 0);
        ignite(ctx, world);
        KIT.shot(ctx, "fire_vanilla");
        ctx.runOnClient(mc -> fx().fireOpacity.set(30));
        ignite(ctx, world);
        check(Math.abs(ctx.computeOnClient(mc -> Effects.fireAlpha(0.9f)) - 0.27f) < 1e-4, "fire alpha at 30 %");
        KIT.shot(ctx, "fire_opacity_30");
        ctx.runOnClient(mc -> {
            pvp().lowFire.set(true);
            pvp().fireHeight.set(60);
        });
        ignite(ctx, world);
        KIT.shot(ctx, "fire_low_and_30");
        world.getServer().runOnServer(s -> s.getPlayerList().getPlayers().forEach(pl -> pl.clearFire()));
        cmd(world, "effect clear @a");
        cmd(world, "gamemode creative @a");
        ctx.runOnClient(mc -> EffectsGameTest.resetAll());
        ctx.waitTicks(5);
    }

    private static void ignite(ClientGameTestContext ctx, TestSingleplayerContext world) {
        world.getServer().runOnServer(s -> s.getPlayerList().getPlayers().forEach(pl -> pl.igniteForSeconds(20)));
        ctx.waitTicks(4);
        check(ctx.computeOnClient(mc -> mc.player.isOnFire()), "player should be burning");
    }

    private static void pumpkin(ClientGameTestContext ctx, TestSingleplayerContext world) {
        cmd(world, "item replace entity @a armor.head with minecraft:carved_pumpkin");
        ctx.waitTicks(10);
        KIT.shot(ctx, "pumpkin_vanilla");
        ctx.runOnClient(mc -> fx().pumpkinOpacity.set(30));
        ctx.waitTicks(2);
        KIT.shot(ctx, "pumpkin_30");
        cmd(world, "item replace entity @a armor.head with minecraft:air");
        ctx.runOnClient(mc -> fx().pumpkinOpacity.set(60));
        ctx.runOnClient(mc -> EffectsPreview.run(EffectsPreview.PUMPKIN));
        ctx.waitTicks(4);
        check(ctx.computeOnClient(mc -> EffectsPreview.pumpkinOn()), "pumpkin preview should run");
        KIT.shotNow(ctx, "pumpkin_preview_60");
        ctx.runOnClient(mc -> EffectsGameTest.resetAll());
    }

    private static void totem(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> fx().totemSize.set(150));
        ctx.runOnClient(mc -> EffectsPreview.run(EffectsPreview.TOTEM));
        ctx.waitTicks(12);
        check(Math.abs(ctx.computeOnClient(mc -> Effects.totemScale()) - 1.5f) < 1e-4, "totem scale 150 %");
        KIT.shotNow(ctx, "totem_150");
        ctx.waitTicks(40);
        ctx.runOnClient(mc -> fx().totemSize.set(40));
        ctx.runOnClient(mc -> EffectsPreview.run(EffectsPreview.TOTEM));
        ctx.waitTicks(12);
        KIT.shotNow(ctx, "totem_40");
        ctx.waitTicks(40);
        ctx.runOnClient(mc -> EffectsGameTest.resetAll());
    }

    private static void blast(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> EffectsPreview.run(EffectsPreview.BLAST));
        ctx.waitTicks(2);
        check(ctx.computeOnClient(mc -> ExplosionShake.energy()) == 0, "no blast shake at 0 %");
        ctx.waitTicks(40);
        ctx.runOnClient(mc -> fx().explosionShake.set(100));
        ctx.runOnClient(mc -> EffectsPreview.run(EffectsPreview.BLAST));
        ctx.waitTicks(3);
        float e = ctx.computeOnClient(mc -> ExplosionShake.energy());
        KIT.log("blast shake energy %.3f", e);
        check(e > 0.2f, "blast shake should start, energy " + e);
        KIT.shotNow(ctx, "blast_shake");
        ctx.waitTicks(60);
        check(ctx.computeOnClient(mc -> ExplosionShake.energy()) < 0.05f, "blast shake should fade");
        ctx.runOnClient(mc -> EffectsGameTest.resetAll());
    }

    private static void hurt(ClientGameTestContext ctx) {
        check(Math.abs(ctx.computeOnClient(mc -> Effects.hurtTilt(1.0)) - 1.0) < 1e-6, "vanilla hurt tilt");
        ctx.runOnClient(mc -> fx().hurtShake.set(30));
        check(Math.abs(ctx.computeOnClient(mc -> Effects.hurtTilt(1.0)) - 0.3) < 1e-6, "hurt tilt at 30 %");
        ctx.runOnClient(mc -> fx().hurtShake.set(100));
        ctx.runOnClient(mc -> EffectsPreview.run(EffectsPreview.HURT));
        ctx.waitTicks(2);
        check(ctx.computeOnClient(mc -> mc.player.hurtTime) > 0, "hurt test should tilt the camera");
        KIT.shotNow(ctx, "hurt_shake_100");
        ctx.waitTicks(15);
        ctx.runOnClient(mc -> EffectsGameTest.resetAll());
    }
}
