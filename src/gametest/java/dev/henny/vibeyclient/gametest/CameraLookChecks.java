package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.ServerRules;
import dev.henny.vibeyclient.module.camera.look.FreeLook;
import dev.henny.vibeyclient.module.pvp.PvpModule;
import dev.henny.vibeyclient.module.pvp.Zoom;
import dev.henny.vibeyclient.api.ModuleManager;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;

import java.util.List;

import static dev.henny.vibeyclient.gametest.CameraGameTest.KIT;
import static dev.henny.vibeyclient.gametest.CameraGameTest.m;
import static dev.henny.vibeyclient.gametest.ReviewKit.check;

final class CameraLookChecks {
    private CameraLookChecks() {}

    static float[] angles(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> new float[] {mc.player.getYRot(), mc.player.getXRot(),
                mc.gameRenderer.getMainCamera().yRot(), mc.gameRenderer.getMainCamera().xRot()});
    }

    static CameraType type(ClientGameTestContext ctx) {
        return ctx.computeOnClient(mc -> mc.options.getCameraType());
    }

    static void drag(ClientGameTestContext ctx, double dx, double dy, int steps) {
        for (int i = 0; i < steps; i++) {
            ctx.getInput().moveCursor(dx, dy);
            ctx.waitTicks(1);
        }
        ctx.waitTicks(3);
    }

    static void freelook(ClientGameTestContext ctx, int[] o) {
        ReviewKit.look(ctx, 180f, 10f);
        ctx.getInput().holdKey(m().freelookKey.mapping());
        ctx.waitTicks(3);
        check(!ctx.computeOnClient(mc -> FreeLook.active()) && type(ctx) == CameraType.FIRST_PERSON, "freelook must be off by default");
        ctx.getInput().releaseKey(m().freelookKey.mapping());
        ctx.runOnClient(mc -> m().freelook.set(true));
        float[] before = angles(ctx);
        ctx.getInput().holdKey(m().freelookKey.mapping());
        ctx.waitTicks(3);
        check(ctx.computeOnClient(mc -> FreeLook.active()), "holding the key should start freelook");
        check(type(ctx) == CameraType.THIRD_PERSON_BACK, "freelook from first person shows the player, got " + type(ctx));
        drag(ctx, 70, -12, 8);
        float[] held = angles(ctx);
        KIT.log("freelook held: player %.2f/%.2f camera %.2f/%.2f (before %.2f/%.2f)", held[0], held[1], held[2], held[3], before[0], before[1]);
        check(Math.abs(held[0] - before[0]) < 0.01f && Math.abs(held[1] - before[1]) < 0.01f, "the player must not turn while freelooking");
        check(Math.abs(held[2] - held[0]) > 15f, "the camera should turn while freelooking");
        KIT.shot(ctx, "freelook_held");
        ctx.getInput().releaseKey(m().freelookKey.mapping());
        ctx.waitTicks(4);
        float[] after = angles(ctx);
        check(!ctx.computeOnClient(mc -> FreeLook.active()), "releasing the key ends freelook");
        check(type(ctx) == CameraType.FIRST_PERSON, "releasing restores first person, got " + type(ctx));
        check(Math.abs(after[2] - after[0]) < 0.01f && Math.abs(after[3] - after[1]) < 0.01f, "camera back on the player's view");
        check(Math.abs(after[0] - before[0]) < 0.01f, "player view unchanged after freelook");
        KIT.shot(ctx, "freelook_released");
        ctx.runOnClient(mc -> m().freelookToggle.set(true));
        ctx.getInput().pressKey(m().freelookKey.mapping());
        ctx.waitTicks(3);
        check(ctx.computeOnClient(mc -> FreeLook.active()), "toggle mode: a press starts freelook");
        ctx.getInput().pressKey(m().freelookKey.mapping());
        ctx.waitTicks(3);
        check(!ctx.computeOnClient(mc -> FreeLook.active()), "toggle mode: a second press ends it");
        ctx.runOnClient(mc -> m().freelookToggle.set(false));
    }

    static void rules(ClientGameTestContext ctx) {
        for (String key : List.of("camera.freelook", "freelook", "camera.*")) {
            ctx.runOnClient(mc -> ServerRules.apply(List.of(key), "No freelook here"));
            ctx.getInput().holdKey(m().freelookKey.mapping());
            ctx.waitTicks(3);
            boolean active = ctx.computeOnClient(mc -> FreeLook.active());
            ctx.getInput().releaseKey(m().freelookKey.mapping());
            ctx.waitTicks(2);
            check(!active, "server rule " + key + " must block freelook");
            check(type(ctx) == CameraType.FIRST_PERSON, "rule " + key + ": view unchanged");
        }
        ctx.runOnClient(mc -> ServerRules.clear());
        KIT.log("rules: camera.freelook, freelook, camera.* block freelook");
    }

    static void zoomTogether(ClientGameTestContext ctx) {
        PvpModule pvp = ModuleManager.get().get(PvpModule.class);
        if (pvp == null || !pvp.isEnabled()) {
            KIT.log("zoom check skipped: pvp module off");
            return;
        }
        float[] before = angles(ctx);
        ctx.getInput().holdKey(pvp.zoomKey.mapping());
        ctx.getInput().holdKey(m().freelookKey.mapping());
        ctx.waitTicks(10);
        drag(ctx, -60, 0, 5);
        float[] held = angles(ctx);
        double zoom = ctx.computeOnClient(mc -> Zoom.factor());
        boolean free = ctx.computeOnClient(mc -> FreeLook.active());
        KIT.shot(ctx, "freelook_with_zoom");
        ctx.getInput().releaseKey(m().freelookKey.mapping());
        ctx.getInput().releaseKey(pvp.zoomKey.mapping());
        ctx.waitTicks(10);
        KIT.log("zoom + freelook: zoom %.2f, freelook %s, player %.2f -> %.2f, camera %.2f", zoom, free, before[0], held[0], held[2]);
        check(free && zoom > 1.5, "zoom and freelook should work together");
        check(Math.abs(held[0] - before[0]) < 0.01f, "player must not turn with zoom + freelook");
        check(ctx.computeOnClient(mc -> Zoom.factor()) < 1.01, "zoom ends after release");
    }
}
