package dev.henny.vibeyclient.gametest;

import dev.henny.companio.mc.CompanioMc;
import dev.henny.companio.world.Companion;
import dev.henny.r3.interact.InteractionState;
import dev.henny.r3.mc.R3Mc;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.world.level.levelgen.Heightmap;
import org.joml.Vector3d;

import static dev.henny.vibeyclient.gametest.CompanionTestKit.*;

public final class CompanionGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        ctx.runOnClient(mc -> {
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
        });
        try (TestSingleplayerContext world = ctx.worldBuilder().setUseConsistentSettings(false)
                .adjustSettings(s -> s.setSeed("vibey-companion")).create()) {
            world.getClientWorld().waitForChunksRender();
            int[] o = scene(ctx, world);
            spawn(ctx, world, o);
            follow(ctx, world, o);
            pet(ctx, world, o);
            rain(ctx, world, o);
            swim(ctx, world, o);
            sleep(ctx, world, o);
            carry(ctx, world, o);
            tricks(ctx, world, o);
            CompanionPackChecks.menu(ctx, world, o);
            CompanionPackChecks.packScreen(ctx);
            CompanionPackChecks.invalidPack(ctx);
        }
    }

    private static int[] scene(ClientGameTestContext ctx, TestSingleplayerContext world) {
        for (String c : new String[] {"gamemode creative @a", "time set 6000", "weather clear", "gamerule advance_time false",
                "gamerule spawn_mobs false", "gamerule send_command_feedback false", "difficulty peaceful"}) cmd(world, c);
        int[] p = ctx.computeOnClient(mc -> {
            int x = (int) Math.floor(mc.player.getX()), z = (int) Math.floor(mc.player.getZ());
            return new int[] {x, mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z};
        });
        int x = p[0], y = Math.max(p[1], 64), z = p[2];
        cmd(world, "fill %d %d %d %d %d %d minecraft:air", x - 14, y, z - 14, x + 14, y + 10, z + 14);
        cmd(world, "fill %d %d %d %d %d %d minecraft:grass_block", x - 14, y - 3, z - 14, x + 14, y - 1, z + 14);
        cmd(world, "fill %d %d %d %d %d %d minecraft:water", x + 5, y - 2, z - 3, x + 10, y - 1, z + 3);
        cmd(world, "fill %d %d %d %d %d %d minecraft:snow_block", x - 14, y - 1, z - 4, x - 2, y - 1, z + 4);
        cmd(world, "fill %d %d %d %d %d %d minecraft:oak_leaves", x - 3, y, z + 8, x + 1, y + 1, z + 9);
        cmd(world, "setworldspawn %d %d %d", x, y, z);
        tp(world, ctx, x + 0.5, y, z + 0.5, 180, 10);
        ctx.waitTicks(20);
        world.getClientWorld().waitForChunksRender();
        log("scene at %d %d %d", x, y, z);
        return new int[] {x, y, z};
    }

    private static void spawn(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        waitFor(ctx, "packs", 400, () -> !CompanioMc.companions().isEmpty());
        ctx.runOnClient(mc -> {
            CompanioMc.config().size = 0;
            CompanioMc.config().name = "";
            check(CompanioMc.select("seal:seal"), "seal is a built-in companion");
        });
        waitFor(ctx, "own companion", 100, () -> CompanioMc.own() != null);
        ctx.waitTicks(40);
        check(own(ctx) != null, "the seal is there");
    }

    private static void follow(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        tp(world, ctx, o[0] + 0.5, o[1], o[2] - 7.5, 180, 15);
        ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        waitFor(ctx, "seal follows", 200, () -> {
            Companion c = CompanioMc.own();
            return c.body.distance(o[0] + 0.5, o[1], o[2] - 7.5) < 4.6;
        });
        ctx.getInput().holdKey(87);
        ctx.waitTicks(16);
        shot(ctx, "follow_walk");
        ctx.getInput().releaseKey(87);
        ctx.waitTicks(30);
        ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
    }

    private static void pet(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        Vector3d c = center(ctx);
        tp(world, ctx, c.x + 2.2, o[1], c.z + 0.2, 90, 20);
        ctx.waitTicks(10);
        lookAt(ctx, center(ctx));
        check(ctx.computeOnClient(mc -> CompanioMc.own() != null && dev.henny.companio.mc.client.CompanioClient.get().target().hovered()),
                "looking at the seal hovers it: " + targetInfo(ctx));
        ctx.getInput().pressMouse(LEFT);
        ctx.waitTicks(10);
        check("petted".equals(behavior(ctx)), "left click pets the seal, behavior " + behavior(ctx));
        lookAt(ctx, center(ctx).add(0, 0.25, 0));
        shot(ctx, "petted");
        ctx.waitTicks(40);
    }

    private static String targetInfo(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> {
            var o = dev.henny.companio.mc.client.CompanioClient.get().target().object();
            var hover = R3Mc.scene().interaction().hover().target();
            Companion c = CompanioMc.own();
            String self = String.format(java.util.Locale.ROOT, "seal %.2f %.2f %.2f eye %.2f %.2f %.2f", c.body.x, c.body.y, c.body.z,
                    mc.player.getEyePosition().x, mc.player.getEyePosition().y, mc.player.getEyePosition().z);
            if (o == null) return "no target object; " + self;
            var p = o.transform().position();
            return String.format(java.util.Locale.ROOT, "target %.2f %.2f %.2f size %.2fx%.2f visible %s anchored %s in scene %s hover %s; %s",
                    p.x, p.y, p.z, o.transform().worldWidth(), o.transform().worldHeight(), o.state().visible(), o.state().anchored(),
                    R3Mc.scene().get(o.id()) != null, hover == null ? "-" : hover.getClass().getSimpleName(), self);
        });
    }

    private static void rain(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        cmd(world, "weather rain");
        waitFor(ctx, "rain hat", 200, () -> CompanioMc.own().propShown("rain_hat"));
        ctx.waitTicks(30);
        lookAt(ctx, center(ctx).add(0, 0.2, 0));
        shot(ctx, "rain_hat");
        cmd(world, "weather clear");
        waitFor(ctx, "rain stops", 200, () -> !CompanioMc.own().propShown("rain_hat"));
    }

    private static void swim(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        tp(world, ctx, o[0] + 4.0, o[1], o[2] + 0.5, -90, 25);
        place(ctx, o[0] + 7.5, o[1], o[2] + 0.5);
        waitFor(ctx, "seal in water", 100, () -> CompanioMc.own().body.inWater);
        ctx.waitTicks(50);
        lookAt(ctx, center(ctx));
        shot(ctx, "swimming");
        tp(world, ctx, o[0] - 2.5, o[1], o[2] + 0.5, 90, 10);
        waitFor(ctx, "seal leaves the water", 400, () -> !CompanioMc.own().body.inWater);
        ctx.waitTicks(10);
        lookAt(ctx, center(ctx));
        shot(ctx, "shake_off");
        ctx.waitTicks(40);
    }

    private static void sleep(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        cmd(world, "time set 18000");
        cmd(world, "effect give @a minecraft:night_vision infinite 0 true");
        ctx.runOnClient(mc -> CompanioMc.own().brain().needs().set("energy", 0.3f));
        waitFor(ctx, "sleeping", 300, () -> {
            Companion c = CompanioMc.own();
            return c.brain().current() != null && c.brain().current().def.id().equals("sleep_night") && c.body.speed() < 0.2;
        });
        ctx.waitTicks(80);
        lookAt(ctx, center(ctx).add(0, 0.3, 0));
        shot(ctx, "sleep_night");
        cmd(world, "time set 6000");
        cmd(world, "effect clear @a");
        ctx.runOnClient(mc -> CompanioMc.own().brain().needs().set("energy", 1f));
        ctx.waitTicks(40);
    }

    private static void carry(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        Vector3d from = center(ctx);
        lookAt(ctx, from);
        ctx.getInput().holdMouse(MIDDLE);
        ctx.waitTicks(4);
        InteractionState s = ctx.computeOnClient(mc -> R3Mc.scene().interaction().state());
        check(s == InteractionState.HOLDING, "middle mouse picks the seal up, state " + s);
        check(ctx.computeOnClient(mc -> CompanioMc.own().held()), "seal is held");
        Vector3d eye = ctx.computeOnClient(mc -> new Vector3d(mc.player.getEyePosition().x, mc.player.getEyePosition().y, mc.player.getEyePosition().z));
        Vector3d to = new Vector3d(eye.x + 2.5, o[1] + 1.3, eye.z - 2.5);
        for (int i = 1; i <= 8; i++) lookAt(ctx, new Vector3d(from).lerp(to, i / 8.0));
        ctx.waitTicks(10);
        shot(ctx, "carried");
        ctx.getInput().releaseMouse(MIDDLE);
        ctx.waitTicks(30);
        Companion c = own(ctx);
        check(!c.held(), "released");
        check(c.body.distance(from.x, from.y, from.z) > 1.5, "the seal was put somewhere else");
        lookAt(ctx, center(ctx));
        shot(ctx, "placed");
    }

    private static void tricks(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        ctx.waitTicks(30);
        check(ctx.computeOnClient(mc -> CompanioMc.trick("ball")), "ball trick starts");
        ctx.waitTicks(46);
        lookAt(ctx, center(ctx).add(0, 0.4, 0));
        shot(ctx, "trick_ball");
        ctx.waitTicks(40);
        tp(world, ctx, o[0] - 9.5, o[1], o[2] + 0.5, 90, 20);
        ctx.runOnClient(mc -> CompanioMc.own().place(o[0] - 12.5, o[1], o[2] + 0.5));
        ctx.waitTicks(20);
        ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        tp(world, ctx, o[0] + 3.5, o[1], o[2] + 0.5, 90, 20);
        ctx.waitTicks(16);
        shot(ctx, "snow_slide");
        ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
        ctx.waitTicks(60);
    }
}
