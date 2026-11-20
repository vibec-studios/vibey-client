package dev.henny.vibeyclient.gametest.media;

import dev.henny.vibeyclient.media.ffmpeg.Ffmpeg;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import org.joml.Vector3d;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

record Room(int x, int y, int z) {
    static Room build(ClientGameTestContext ctx, TestSingleplayerContext world) {
        int[] p = ctx.computeOnClient(mc -> new int[] {(int) Math.floor(mc.player.getX()), 120, (int) Math.floor(mc.player.getZ())});
        Room r = new Room(p[0], p[1], p[2]);
        for (String c : new String[] {"gamemode creative @a", "time set 6000", "weather clear", "gamerule fallDamage false",
                "gamerule doDaylightCycle false", "gamerule doMobSpawning false"}) world.getServer().runCommand(c);
        r.cmd(world, "fill %d %d %d %d %d %d minecraft:air", r.x - 7, r.y, r.z - 7, r.x + 7, r.y + 7, r.z + 7);
        r.cmd(world, "fill %d %d %d %d %d %d minecraft:smooth_stone", r.x - 7, r.y - 1, r.z - 7, r.x + 7, r.y - 1, r.z + 7);
        r.cmd(world, "fill %d %d %d %d %d %d minecraft:white_concrete", r.x - 5, r.y, r.z - 5, r.x + 5, r.y + 6, r.z - 5);
        r.cmd(world, "fill %d %d %d %d %d %d minecraft:light_gray_concrete", r.x + 5, r.y, r.z - 5, r.x + 5, r.y + 6, r.z + 5);
        r.cmd(world, "fill %d %d %d %d %d %d minecraft:gray_concrete", r.x - 5, r.y, r.z - 5, r.x - 5, r.y + 6, r.z + 5);
        r.cmd(world, "tp @a %.1f %d %.1f 180 0", r.x + 0.5, r.y, r.z + 0.5);
        ctx.waitTicks(30);
        world.getClientWorld().waitForChunksRender();
        return r;
    }

    Vector3d northWall(double dx, double y) {
        return new Vector3d(x + 0.5 + dx, this.y + y, z - 4);
    }

    Vector3d eastWall(double y, double dz) {
        return new Vector3d(x + 5, this.y + y, z + 0.5 + dz);
    }

    Vector3d westWall(double y, double dz) {
        return new Vector3d(x - 4, this.y + y, z + 0.5 + dz);
    }

    void cmd(TestSingleplayerContext world, String format, Object... args) {
        world.getServer().runCommand(String.format(Locale.ROOT, format, args));
    }

    static Path testVideo(String name, String pattern, int frequency) {
        Path out = Film.root().resolve("build/review/media-r3/media/" + name);
        if (Files.exists(out)) return out;
        String ff = Ffmpeg.find();
        if (ff == null) return null;
        try {
            Files.createDirectories(out.getParent());
            Process p = new ProcessBuilder(List.of(ff, "-y", "-hide_banner", "-v", "error",
                    "-f", "lavfi", "-i", pattern + "=size=640x360:rate=30:duration=30",
                    "-f", "lavfi", "-i", "sine=frequency=" + frequency + ":beep_factor=4:duration=30",
                    "-c:v", "libx264", "-pix_fmt", "yuv420p", "-c:a", "aac", "-shortest", out.toString()))
                    .redirectErrorStream(true).start();
            p.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
            if (!p.waitFor(60, TimeUnit.SECONDS) || p.exitValue() != 0) return null;
            return out;
        } catch (Exception e) {
            return null;
        }
    }
}
