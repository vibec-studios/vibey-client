package dev.henny.vibeyclient.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import org.joml.Vector3d;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Consumer;

final class ReviewKit {
    static final int LEFT = 0, RIGHT = 1, MIDDLE = 2;

    private final String area;
    private final String prefix;
    private int shots;

    ReviewKit(String area) {
        this.area = area;
        this.prefix = "[vibeyclient-" + area + "-test] ";
    }

    void log(String format, Object... args) {
        System.out.println(prefix + String.format(Locale.ROOT, format, args));
    }

    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    static void cmd(TestSingleplayerContext world, String format, Object... args) {
        world.getServer().runCommand(args.length == 0 ? format : String.format(Locale.ROOT, format, args));
    }

    void shot(ClientGameTestContext ctx, String name) {
        ctx.runOnClient(mc -> mc.gui.getChat().clearMessages(false));
        ctx.waitTicks(2);
        shotNow(ctx, name);
    }

    void shotNow(ClientGameTestContext ctx, String name) {
        String full = String.format(Locale.ROOT, "%02d_%s", ++shots, name);
        Path file = ctx.takeScreenshot(area + "_" + full);
        Path review = root().resolve("build/review/" + area);
        try {
            Files.createDirectories(review);
            Files.copy(file, review.resolve(full + ".png"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new AssertionError("copy screenshot " + name, e);
        }
    }

    static void lookAt(ClientGameTestContext ctx, Vector3d target) {
        float[] r = ctx.computeOnClient(mc -> {
            var e = mc.player.getEyePosition();
            double dx = target.x - e.x, dy = target.y - e.y, dz = target.z - e.z;
            return new float[] {(float) Math.toDegrees(Math.atan2(-dx, dz)),
                    (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)))};
        });
        look(ctx, r[0], r[1]);
    }

    static void look(ClientGameTestContext ctx, float yaw, float pitch) {
        ctx.runOnClient(mc -> {
            mc.player.setYRot(yaw);
            mc.player.setXRot(pitch);
            mc.player.yRotO = yaw;
            mc.player.xRotO = pitch;
            mc.player.setYHeadRot(yaw);
        });
        ctx.waitTicks(4);
    }

    static int[] platform(ClientGameTestContext ctx, TestSingleplayerContext world, String floor) {
        int[] p = ctx.computeOnClient(mc -> new int[] {(int) Math.floor(mc.player.getX()), 120, (int) Math.floor(mc.player.getZ())});
        int x = p[0], y = p[1], z = p[2];
        for (String c : new String[] {"gamemode creative @a", "time set 6000", "weather clear", "gamerule fall_damage false",
                "gamerule advance_time false", "gamerule spawn_mobs false", "difficulty peaceful"}) cmd(world, c);
        cmd(world, "fill %d %d %d %d %d %d minecraft:air", x - 8, y, z - 8, x + 8, y + 8, z + 8);
        cmd(world, "fill %d %d %d %d %d %d minecraft:%s", x - 8, y - 1, z - 8, x + 8, y - 1, z + 8, floor);
        cmd(world, "tp @a %.1f %d %.1f 180 0", x + 0.5, y, z + 0.5);
        ctx.waitTicks(30);
        world.getClientWorld().waitForChunksRender();
        return new int[] {x, y, z};
    }

    static void realFrameRate(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> {
            mc.options.inactivityFpsLimit().set(net.minecraft.client.InactivityFpsLimit.MINIMIZED);
            mc.options.enableVsync().set(false);
            mc.getWindow().updateVsync(false);
            mc.options.framerateLimit().set(260);
        });
    }

    static void normalFrameRate(ClientGameTestContext ctx) {
        ctx.runOnClient(mc -> mc.options.framerateLimit().set(120));
    }

    double[] frames(ClientGameTestContext ctx, String label, int ticks) {
        ctx.runOnClient(mc -> FrameClock.start(20000));
        ctx.waitTicks(ticks);
        double[] r = ctx.computeOnClient(mc -> FrameClock.stop());
        log("frames %s: %.0f frames, avg %.3f ms (%.0f fps), p99 %.2f ms", label, r.length > 2 ? r[2] : 0, r[0],
                r[0] > 0 ? 1000 / r[0] : 0, r[1]);
        return r;
    }

    double[] compare(ClientGameTestContext ctx, String what, int rounds, Consumer<Boolean> setOn) {
        double[] off = new double[rounds], on = new double[rounds];
        for (int i = 0; i < rounds; i++) {
            ctx.runOnClient(mc -> setOn.accept(false));
            ctx.waitTicks(20);
            off[i] = frames(ctx, what + " off#" + i, 120)[0];
            ctx.runOnClient(mc -> setOn.accept(true));
            ctx.waitTicks(20);
            on[i] = frames(ctx, what + " on#" + i, 120)[0];
        }
        Arrays.sort(off);
        Arrays.sort(on);
        double mOff = off[rounds / 2], mOn = on[rounds / 2];
        log("FRAMES %s median off %.3f ms, on %.3f ms (%+.1f %%)", what, mOff, mOn, mOff > 0 ? (mOn / mOff - 1) * 100 : 0);
        return new double[] {mOff, mOn};
    }

    static Path root() {
        Path p = Path.of("").toAbsolutePath();
        while (p != null && !Files.exists(p.resolve("stonecutter.gradle"))) p = p.getParent();
        return p == null ? Path.of("").toAbsolutePath() : p;
    }
}
