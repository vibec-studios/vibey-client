package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.module.clips.ClipsModule;
import dev.henny.vibeyclient.module.clips.shots.GalleryController;
import dev.henny.vibeyclient.module.clips.shots.ShotRow;
import dev.henny.vibeyclient.module.clips.shots.Shots;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import java.util.function.BooleanSupplier;

import static dev.henny.vibeyclient.gametest.ReviewKit.check;
import static dev.henny.vibeyclient.gametest.ReviewKit.cmd;

public final class GalleryGameTest implements FabricClientGameTest {
    static final ReviewKit KIT = new ReviewKit("gallery");

    static GalleryController c() {
        return Shots.controller();
    }

    static String selected() {
        ShotRow r = c().sel.peek();
        return r == null ? null : r.id;
    }

    static void waitFor(ClientGameTestContext ctx, String what, int maxTicks, BooleanSupplier ok) {
        for (int i = 0; i < maxTicks; i++) {
            if (ctx.computeOnClient(mc -> ok.getAsBoolean())) return;
            ctx.waitTicks(1);
        }
        throw new AssertionError("timed out: " + what);
    }

    @Override
    public void runTest(ClientGameTestContext ctx) {
        ctx.getInput().resizeWindow(1280, 720);
        ctx.runOnClient(mc -> {
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
            GlobalSettings.language.set(GlobalSettings.Language.ENGLISH);
            check(ModuleManager.get().get(ClipsModule.class).isEnabled(), "clips module should be on");
        });
        GalleryFixtures.isolate();
        try (TestSingleplayerContext world = ctx.worldBuilder().setUseConsistentSettings(false)
                .adjustSettings(s -> s.setSeed("vibeycamera")).create()) {
            world.getClientWorld().waitForChunksRender();
            int[] o = room(ctx, world);
            GalleryFixtures.add(ctx.takeScreenshot("gallery_world_source"), "2025-10-20_12.00.00.png");
            GalleryScreenChecks.screen(ctx);
            GalleryPanelChecks.panel(ctx, o);
        } finally {
            ctx.runOnClient(mc -> Shots.removePanel());
            GalleryFixtures.release();
        }
        ctx.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.AUTO));
    }

    private static int[] room(ClientGameTestContext ctx, TestSingleplayerContext world) {
        int[] o = ReviewKit.platform(ctx, world, "smooth_stone");
        int x = o[0], y = o[1], z = o[2];
        cmd(world, "gamerule send_command_feedback false");
        cmd(world, "fill %d %d %d %d %d %d minecraft:white_concrete", x - 5, y, z - 5, x + 5, y + 5, z - 5);
        cmd(world, "fill %d %d %d %d %d %d minecraft:light_gray_concrete", x + 5, y, z - 5, x + 5, y + 5, z + 5);
        ctx.waitTicks(10);
        world.getClientWorld().waitForChunksRender();
        return o;
    }
}
