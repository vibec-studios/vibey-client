package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.module.info.InfoModule;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import org.joml.Vector3d;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

final class InfoTestKit {
    static final String P = "[vibeyclient-info-test] ";
    static final int MIDDLE = 2;
    private static int shots;

    private InfoTestKit() {}

    static InfoModule m() {
        return ModuleManager.get().get(InfoModule.class);
    }

    static void cmd(TestSingleplayerContext world, String format, Object... args) {
        world.getServer().runCommand(args.length == 0 ? format : String.format(Locale.ROOT, format, args));
    }

    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    static void log(String format, Object... args) {
        System.out.println(P + String.format(Locale.ROOT, format, args));
    }

    static void lookAt(ClientGameTestContext ctx, Vector3d target) {
        float[] r = ctx.computeOnClient(mc -> {
            var e = mc.player.getEyePosition();
            double dx = target.x - e.x, dy = target.y - e.y, dz = target.z - e.z;
            return new float[] {(float) Math.toDegrees(Math.atan2(-dx, dz)),
                    (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)))};
        });
        ctx.runOnClient(mc -> {
            mc.player.setYRot(r[0]);
            mc.player.setXRot(r[1]);
            mc.player.yRotO = r[0];
            mc.player.xRotO = r[1];
            mc.player.setYHeadRot(r[0]);
        });
        ctx.waitTicks(4);
    }

    static void turn(ClientGameTestContext ctx, Vector3d from, Vector3d to, int steps) {
        for (int i = 1; i <= steps; i++) lookAt(ctx, new Vector3d(from).lerp(to, i / (double) steps));
    }

    static void hold(ClientGameTestContext ctx, int key, int ticks) {
        ctx.getInput().holdKey(key);
        ctx.waitTicks(ticks);
        ctx.getInput().releaseKey(key);
        ctx.waitTicks(2);
    }

    static void shot(ClientGameTestContext ctx, String name) {
        ctx.runOnClient(mc -> mc.gui.getChat().clearMessages(false));
        ctx.waitTicks(2);
        String full = String.format(Locale.ROOT, "%02d_%s", ++shots, name);
        Path file = ctx.takeScreenshot("info_" + full);
        Path review = root().resolve("build/review/info");
        try {
            Files.createDirectories(review);
            Files.copy(file, review.resolve(full + ".png"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new AssertionError("copy screenshot " + name, e);
        }
    }

    private static Path root() {
        Path p = Path.of("").toAbsolutePath();
        while (p != null && !Files.exists(p.resolve("stonecutter.gradle"))) p = p.getParent();
        return p == null ? Path.of("").toAbsolutePath() : p;
    }
}
