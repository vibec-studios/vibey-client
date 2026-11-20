package dev.henny.vibeyclient.gametest.media;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

final class Film {
    static final String REVIEW = "build/review/media-r3";

    private static boolean rolling;
    private static int frame;

    private Film() {}

    static void start() {
        rolling = true;
        frame = 0;
        try {
            Path dir = frames();
            if (Files.isDirectory(dir)) {
                try (var files = Files.list(dir)) {
                    for (Path p : files.toList()) Files.deleteIfExists(p);
                }
            }
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new AssertionError("film dir", e);
        }
    }

    static void stop() {
        rolling = false;
    }

    static void wait(ClientGameTestContext ctx, int ticks) {
        if (!rolling) {
            ctx.waitTicks(ticks);
            return;
        }
        for (int i = 0; i < ticks; i++) {
            Path file = ctx.takeScreenshot("media_r3_film");
            copy(file, frames().resolve(String.format(Locale.ROOT, "f%04d.png", frame++)));
        }
    }

    static void shot(ClientGameTestContext ctx, String name) {
        ctx.runOnClient(mc -> mc.gui.getChat().clearMessages(false));
        wait(ctx, 2);
        Path file = ctx.takeScreenshot("media_r3_" + name);
        copy(file, review().resolve(name + ".png"));
    }

    static Path review() {
        return root().resolve(REVIEW);
    }

    static Path frames() {
        return review().resolve("frames");
    }

    private static void copy(Path from, Path to) {
        try {
            Files.createDirectories(to.getParent());
            Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new AssertionError("copy screenshot " + to, e);
        }
    }

    static Path root() {
        Path p = Path.of("").toAbsolutePath();
        while (p != null && !Files.exists(p.resolve("stonecutter.gradle"))) p = p.getParent();
        return p == null ? Path.of("").toAbsolutePath() : p;
    }
}
