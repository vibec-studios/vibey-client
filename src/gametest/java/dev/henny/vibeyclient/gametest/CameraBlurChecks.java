package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.module.camera.blur.MotionBlur;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;

import static dev.henny.vibeyclient.gametest.CameraGameTest.KIT;
import static dev.henny.vibeyclient.gametest.CameraGameTest.m;
import static dev.henny.vibeyclient.gametest.ReviewKit.check;

final class CameraBlurChecks {
    private CameraBlurChecks() {}

    static void blur(ClientGameTestContext ctx, int[] o) {
        ctx.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
        spinShot(ctx, "blur_off_turning");
        ctx.runOnClient(mc -> {
            m().motionBlur.set(true);
            m().blurStrength.set(10);
        });
        ctx.waitTicks(10);
        long frames = ctx.computeOnClient(mc -> MotionBlur.frames());
        check(!ctx.computeOnClient(mc -> MotionBlur.unavailable()), "motion blur post chain should load");
        check(frames > 0, "motion blur should run every frame");
        spinShot(ctx, "blur_on_turning_strength10");
        ctx.waitTicks(20);
        float weight = ctx.computeOnClient(mc -> MotionBlur.lastWeight());
        boolean fixed = ctx.computeOnClient(mc -> MotionBlur.strengthFixed());
        KIT.log("blur: frames %d, weight %.3f, strength fixed %s", frames, weight, fixed);
        check(weight > 0.3f && !fixed, "strength 10 should give a strong history weight, got " + weight);
        KIT.shot(ctx, "blur_on_still");
        ctx.runOnClient(mc -> m().blurStrength.set(3));
        spinShot(ctx, "blur_on_turning_strength3");
        ReviewKit.realFrameRate(ctx);
        KIT.compare(ctx, "motion blur", 3, on -> m().motionBlur.set(on));
        ReviewKit.normalFrameRate(ctx);
        ctx.runOnClient(mc -> m().motionBlur.set(false));
    }

    private static void spinShot(ClientGameTestContext ctx, String name) {
        ReviewKit.look(ctx, 150f, 5f);
        ctx.runOnClient(mc -> mc.gui.getChat().clearMessages(false));
        for (int i = 0; i < 10; i++) {
            ctx.runOnClient(mc -> {
                float y = mc.player.getYRot() + 6f;
                mc.player.setYRot(y);
                mc.player.setYHeadRot(y);
            });
            ctx.waitTicks(1);
        }
        KIT.shotNow(ctx, name);
    }
}
