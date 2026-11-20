package dev.henny.cosmetico.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class StandaloneShots {
    private final ClientGameTestContext context;
    private final Path out;

    StandaloneShots(ClientGameTestContext context) {
        this.context = context;
        this.out = repoRoot().resolve("build/review/cosmetico/standalone");
    }

    void take(String name) {
        Path shot = context.takeScreenshot(name);
        try {
            Files.createDirectories(out);
            Files.copy(shot, out.resolve(name + ".png"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("copy " + shot, e);
        }
        System.out.println("[cosmetico-standalone] shot " + out.resolve(name + ".png"));
    }

    private static Path repoRoot() {
        Path p = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path q = p; q != null; q = q.getParent()) {
            if (Files.exists(q.resolve("settings.gradle")) && Files.isDirectory(q.resolve("libs"))) return q;
        }
        return p;
    }
}
