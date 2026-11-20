package dev.henny.vibeyclient.gametest;

import dev.henny.companio.mc.CompanioMc;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import org.joml.Vector3d;

import static dev.henny.vibeyclient.gametest.CompanionTestKit.*;
import static dev.henny.vibeyclient.gametest.PackShots.*;

final class RobotScenes {
    private RobotScenes() {}

    static void run(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        tp(world, ctx, o[0] + 0.5, o[1], o[2] + 0.5, 180, 10);
        select(ctx, "robot:robot");
        follow(ctx, world, o);
        scan(ctx, world, o);
        rain(ctx, world, o);
        sun(ctx, world, o);
        tricks(ctx, world, o);
        music(ctx, world, o);
        night(ctx, world, o);
        variants(ctx, world, o);
    }

    private static void follow(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        tp(world, ctx, o[0] + 0.5, o[1], o[2] + 8.5, 0, 15);
        ctx.waitTicks(16);
        lookAt(ctx, center(ctx).add(0, 0.3, 0));
        snap(ctx, "robot_follow");
        waitFor(ctx, "robot follows", 200, () -> CompanioMc.own().body.distance(o[0] + 0.5, o[1], o[2] + 8.5) < 4.8);
        ctx.waitTicks(30);
        face(ctx, world, 2.2, 0.3, 0);
        PackShots.pet(ctx);
        lookAt(ctx, center(ctx).add(0, 0.2, 0));
        snap(ctx, "robot_petted");
        ctx.waitTicks(30);
    }

    private static void scan(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        tp(world, ctx, o[0] + 0.5, o[1], o[2] - 12.0, 180, 10);
        place(ctx, o[0] + 2.5, o[1], o[2] - 13.0);
        ctx.runOnClient(mc -> CompanioMc.command(dev.henny.companio.world.Command.STAY));
        ctx.waitTicks(40);
        lookAt(ctx, new Vector3d(o[0] - 1.5, o[1] + 1.5, o[2] - 15.5));
        ctx.waitTicks(10);
        lookAt(ctx, new Vector3d(o[0] + 0.5, o[1] + 1.5, o[2] - 15.5));
        waitBehavior(ctx, "scan", 60);
        ctx.waitTicks(14);
        snap(ctx, "robot_scan_diamond");
        ctx.waitTicks(60);
        ctx.runOnClient(mc -> CompanioMc.command(dev.henny.companio.world.Command.FOLLOW));
        ctx.waitTicks(20);
    }

    private static void rain(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        tp(world, ctx, o[0] + 0.5, o[1], o[2] + 6.5, 0, 15);
        ctx.waitTicks(40);
        cmd(world, "weather rain");
        waitFor(ctx, "robot umbrella", 200, () -> CompanioMc.own().propShown("umbrella"));
        ctx.waitTicks(30);
        face(ctx, world, 2.6, 0.8, 0.3);
        snap(ctx, "robot_umbrella");
        cmd(world, "weather thunder");
        waitBehavior(ctx, "storm", 200);
        ctx.waitTicks(20);
        face(ctx, world, 2.4, 0.6, 0);
        snap(ctx, "robot_thunder");
        cmd(world, "weather clear");
        ctx.waitTicks(60);
    }

    private static void sun(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        ctx.runOnClient(mc -> CompanioMc.own().brain().needs().set("energy", 0.3f));
        waitBehavior(ctx, "solar", 400);
        ctx.waitTicks(40);
        face(ctx, world, -0.6, 2.4, 0.6);
        snap(ctx, "robot_solar");
        ctx.runOnClient(mc -> CompanioMc.own().brain().needs().set("energy", 1f));
        ctx.waitTicks(40);
    }

    private static void tricks(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        check(ctx.computeOnClient(mc -> CompanioMc.trick("fold")), "fold trick starts");
        ctx.waitTicks(24);
        face(ctx, world, 2.4, 0.6, 0);
        snap(ctx, "robot_fold");
        ctx.waitTicks(60);
        check(ctx.computeOnClient(mc -> CompanioMc.trick("jump")), "jump trick starts");
        ctx.waitTicks(14);
        face(ctx, world, 2.8, 0.8, 0.4);
        snap(ctx, "robot_jump_boost");
        ctx.waitTicks(60);
        cmd(world, "xp add @a 3 levels");
        waitBehavior(ctx, "level_up", 60);
        ctx.waitTicks(16);
        face(ctx, world, 2.4, 0.6, 0);
        snap(ctx, "robot_level_up");
        ctx.waitTicks(60);
    }

    private static void music(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        tp(world, ctx, o[0] + 0.5, o[1], o[2] + 6.5, 0, 15);
        ctx.waitTicks(60);
        cmd(world, "playsound minecraft:music_disc.cat record @a %d %d %d 1 1", o[0], o[1] + 1, o[2] + 3);
        waitBehavior(ctx, "dance", 120);
        ctx.waitTicks(20);
        face(ctx, world, 2.4, 0.6, 0);
        snap(ctx, "robot_dance");
        cmd(world, "stopsound @a record");
        ctx.waitTicks(40);
    }

    private static void night(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        cmd(world, "time set 18000");
        waitFor(ctx, "headlamp", 200, () -> CompanioMc.own().propShown("lamp"));
        ctx.waitTicks(40);
        face(ctx, world, 2.6, 0.9, 0.2);
        snap(ctx, "robot_night_lamp");
        cmd(world, "time set 6000");
        ctx.waitTicks(40);
    }

    private static void variants(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        for (String v : new String[] {"iron", "retro"}) {
            variant(ctx, v);
            ctx.waitTicks(37);
            face(ctx, world, 2.2, 0.5, 0);
            log("robot %s: behavior %s props %s layers %s", v, behavior(ctx), ctx.computeOnClient(mc -> CompanioMc.own().def.props().keySet().stream()
                    .filter(p -> CompanioMc.own().propShown(p)).toList()), ctx.computeOnClient(mc -> CompanioMc.own().animator.toString()));
            snap(ctx, "robot_" + v);
        }
        variant(ctx, "copper");
    }
}
