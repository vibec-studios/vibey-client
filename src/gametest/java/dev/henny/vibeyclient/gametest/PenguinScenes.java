package dev.henny.vibeyclient.gametest;

import dev.henny.companio.mc.CompanioMc;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import static dev.henny.vibeyclient.gametest.CompanionTestKit.*;
import static dev.henny.vibeyclient.gametest.PackShots.*;

final class PenguinScenes {
    private PenguinScenes() {}

    static void run(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        select(ctx, "penguin:penguin");
        follow(ctx, world, o);
        pet(ctx, world, o);
        rain(ctx, world, o);
        swim(ctx, world, o);
        snow(ctx, world, o);
        tricks(ctx, world, o);
        desert(ctx, world, o);
        night(ctx, world, o);
        variants(ctx, world, o);
    }

    private static void follow(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        tp(world, ctx, o[0] + 0.5, o[1], o[2] - 8.5, 180, 15);
        ctx.waitTicks(16);
        lookAt(ctx, center(ctx).add(0, 0.3, 0));
        snap(ctx, "penguin_follow");
        waitFor(ctx, "penguin follows", 200, () -> CompanioMc.own().body.distance(o[0] + 0.5, o[1], o[2] - 8.5) < 4.8);
        ctx.waitTicks(30);
    }

    private static void pet(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        face(ctx, world, 2.2, 0.3, 0);
        PackShots.pet(ctx);
        lookAt(ctx, center(ctx).add(0, 0.2, 0));
        snap(ctx, "penguin_petted");
        ctx.waitTicks(30);
    }

    private static void rain(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        tp(world, ctx, o[0] + 3.5, o[1], o[2] - 6.0, 180, 20);
        place(ctx, o[0] + 3.0, o[1], o[2] - 9.5);
        ctx.waitTicks(20);
        cmd(world, "weather rain");
        waitFor(ctx, "umbrella", 200, () -> CompanioMc.own().propShown("umbrella"));
        ctx.waitTicks(34);
        face(ctx, world, 2.8, 0.9, 0.3);
        snap(ctx, "penguin_umbrella");
        cmd(world, "weather clear");
        waitBehavior(ctx, "umbrella_close", 200);
        ctx.waitTicks(16);
        lookAt(ctx, center(ctx).add(0, 0.3, 0));
        snap(ctx, "penguin_umbrella_shake");
        ctx.waitTicks(40);
    }

    private static void swim(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        tp(world, ctx, o[0] + 4.0, o[1] + 1, o[2] + 0.5, -90, 25);
        place(ctx, o[0] + 7.5, o[1], o[2] + 0.5);
        waitFor(ctx, "penguin in water", 100, () -> CompanioMc.own().body.inWater);
        ctx.waitTicks(30);
        lookAt(ctx, center(ctx));
        snap(ctx, "penguin_swim");
        tp(world, ctx, o[0] - 2.5, o[1], o[2] + 0.5, 90, 10);
        waitFor(ctx, "penguin leaves the water", 400, () -> !CompanioMc.own().body.inWater);
        ctx.waitTicks(30);
    }

    private static void snow(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        tp(world, ctx, o[0] - 6.5, o[1], o[2] + 0.5, 90, 20);
        place(ctx, o[0] - 9.5, o[1], o[2] + 0.5);
        waitFor(ctx, "scarf in the cold", 200, () -> CompanioMc.own().propShown("scarf"));
        ctx.waitTicks(30);
        force(ctx, "snow_play");
        ctx.waitTicks(30);
        face(ctx, world, 2.6, 1.0, 0);
        snap(ctx, "penguin_snowball");
        ctx.waitTicks(50);
        tp(world, ctx, o[0] - 8.5, o[1], o[2] + 8.0, 90, 25);
        place(ctx, o[0] - 12.5, o[1], o[2] + 8.0);
        ctx.waitTicks(10);
        tp(world, ctx, o[0] - 2.5, o[1], o[2] + 8.0, 90, 25);
        ctx.waitTicks(12);
        lookAt(ctx, center(ctx));
        log("ice: behavior %s", behavior(ctx));
        snap(ctx, "penguin_ice_slide");
        ctx.waitTicks(60);
    }

    private static void tricks(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        tp(world, ctx, o[0] + 0.5, o[1], o[2] - 6.5, 0, 20);
        ctx.waitTicks(60);
        check(ctx.computeOnClient(mc -> CompanioMc.trick("juggle")), "juggle trick starts");
        ctx.waitTicks(26);
        face(ctx, world, 2.6, 0.4, 0);
        snap(ctx, "penguin_juggle");
        ctx.waitTicks(80);
        cmd(world, "xp add @a 2 levels");
        waitBehavior(ctx, "celebrate_level", 60);
        ctx.waitTicks(8);
        face(ctx, world, 2.6, 0.4, 0);
        snap(ctx, "penguin_level_up");
        ctx.waitTicks(60);
        check(ctx.computeOnClient(mc -> CompanioMc.trick("bow")), "bow trick starts");
        ctx.waitTicks(16);
        face(ctx, world, 2.4, 0.8, 0);
        snap(ctx, "penguin_bow");
        ctx.waitTicks(40);
    }

    private static void desert(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        tp(world, ctx, o[0] + 14.5, o[1], o[2] - 14.5, 0, 20);
        place(ctx, o[0] + 16.5, o[1], o[2] - 12.5);
        waitFor(ctx, "sunglasses in the desert", 200, () -> CompanioMc.own().propShown("shades"));
        ctx.waitTicks(20);
        force(ctx, "hot_fan");
        ctx.waitTicks(20);
        face(ctx, world, 2.4, 0.6, 0);
        snap(ctx, "penguin_desert");
        ctx.waitTicks(20);
        tp(world, ctx, o[0] + 0.5, o[1], o[2] - 4.5, 0, 20);
        ctx.waitTicks(80);
    }

    private static void night(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        cmd(world, "time set 13600");
        cmd(world, "effect give @a minecraft:night_vision infinite 0 true");
        ctx.runOnClient(mc -> CompanioMc.own().brain().needs().set("energy", 0.3f));
        waitFor(ctx, "penguin sleeps", 300, () -> CompanioMc.own().brain().current() != null
                && CompanioMc.own().brain().current().def.id().equals("sleep_night") && CompanioMc.own().body.speed() < 0.2);
        ctx.waitTicks(50);
        face(ctx, world, 2.2, 0.8, 0);
        snap(ctx, "penguin_sleep");
        cmd(world, "time set 6000");
        cmd(world, "effect clear @a");
        ctx.runOnClient(mc -> CompanioMc.own().brain().needs().set("energy", 1f));
        ctx.waitTicks(40);
    }

    private static void variants(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        for (String v : new String[] {"emperor", "rockhopper"}) {
            variant(ctx, v);
            ctx.waitTicks(20);
            face(ctx, world, 2.2, 0.5, 0);
            snap(ctx, "penguin_" + v);
        }
        variant(ctx, "classic");
    }
}
