package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.module.camera.look.Orbit;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.world.item.Items;
import org.joml.Vector3d;

import java.util.List;

import static dev.henny.vibeyclient.gametest.CameraGameTest.KIT;
import static dev.henny.vibeyclient.gametest.CameraLookChecks.angles;
import static dev.henny.vibeyclient.gametest.CameraLookChecks.drag;
import static dev.henny.vibeyclient.gametest.ReviewKit.MIDDLE;
import static dev.henny.vibeyclient.gametest.ReviewKit.check;
import static dev.henny.vibeyclient.gametest.ReviewKit.cmd;

final class CameraOrbitChecks {
    private CameraOrbitChecks() {}

    static void orbit(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        ReviewKit.look(ctx, 180f, 15f);
        ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        ctx.waitTicks(3);
        KIT.shot(ctx, "orbit_before");
        float[] before = angles(ctx);
        ctx.getInput().holdMouse(MIDDLE);
        ctx.waitTicks(2);
        check(ctx.computeOnClient(mc -> Orbit.capturing()), "middle mouse in third person should start the orbit drag");
        drag(ctx, 150, 0, 8);
        ctx.getInput().releaseMouse(MIDDLE);
        ctx.waitTicks(3);
        float[] after = angles(ctx);
        float offset = ctx.computeOnClient(mc -> Orbit.yawOffset());
        KIT.log("orbit: player %.2f -> %.2f, camera %.2f, offset %.2f", before[0], after[0], after[2], offset);
        check(Math.abs(after[0] - before[0]) < 0.01f, "the player must not turn while orbiting");
        check(Math.abs(offset) > 120f, "dragging should rotate the camera around the player, offset " + offset);
        check(Math.abs(after[2] - (after[0] + offset)) < 0.5f, "camera yaw = player yaw + orbit offset");
        KIT.shot(ctx, "orbit_front_skin");
        ctx.getInput().holdMouse(MIDDLE);
        ctx.waitTicks(2);
        ctx.getInput().scroll(-3);
        ctx.waitTicks(2);
        drag(ctx, -40, 20, 3);
        ctx.getInput().releaseMouse(MIDDLE);
        ctx.waitTicks(3);
        float zoom = ctx.computeOnClient(mc -> Orbit.zoom());
        check(zoom > 1.4f, "wheel while dragging moves the camera away, zoom " + zoom);
        KIT.shot(ctx, "orbit_wheel_far");
        pickBlockTap(ctx, world, o);
        blockedByRule(ctx);
        ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
        ctx.waitTicks(3);
        check(ctx.computeOnClient(mc -> Orbit.zoom() == 1f && Orbit.yawOffset() == 0f), "first person resets the orbit");
    }

    private static void pickBlockTap(ClientGameTestContext ctx, TestSingleplayerContext world, int[] o) {
        cmd(world, "clear @a");
        ctx.waitTicks(3);
        ReviewKit.lookAt(ctx, new Vector3d(o[0] + 0.5, o[1] + 0.5, o[2] - 1.0));
        ctx.getInput().pressMouse(MIDDLE);
        ctx.waitTicks(8);
        boolean picked = ctx.computeOnClient(mc -> mc.player.getMainHandItem().is(Items.DIAMOND_BLOCK));
        String hand = ctx.computeOnClient(mc -> mc.player.getMainHandItem().toString());
        KIT.log("middle tap in third person picked: %s", hand);
        check(picked, "a middle tap (no drag) must still pick the block");
    }

    private static void blockedByRule(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> ServerRules.apply(List.of("camera.orbit"), "No orbit"));
        ctx.getInput().holdMouse(MIDDLE);
        ctx.waitTicks(2);
        boolean capturing = ctx.computeOnClient(mc -> Orbit.capturing());
        ctx.getInput().releaseMouse(MIDDLE);
        ctx.waitTicks(2);
        ctx.runOnClient(mc -> ServerRules.clear());
        check(!capturing, "server rule camera.orbit blocks the orbit drag");
    }
}
