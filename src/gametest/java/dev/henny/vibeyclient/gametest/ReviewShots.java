package dev.henny.vibeyclient.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

final class ReviewShots {
    private final String dir;
    private int count;

    ReviewShots(String dir) {
        this.dir = dir;
    }

    void shot(ClientGameTestContext ctx, String name) {
        ctx.runOnClient(mc -> mc.gui.getChat().clearMessages(false));
        ctx.waitTicks(2);
        String full = String.format(Locale.ROOT, "%02d_%s", ++count, name);
        Path file = ctx.takeScreenshot(dir + "_" + full);
        Path review = root().resolve("build/review/" + dir);
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
