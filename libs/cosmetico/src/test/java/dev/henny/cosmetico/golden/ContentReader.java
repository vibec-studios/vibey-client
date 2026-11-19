package dev.henny.cosmetico.golden;

import dev.henny.cosmetico.thumbs.ResourceReader;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ContentReader {
    private ContentReader() {}

    public static ResourceReader dist(String root) {
        Path dist = Golden.root().resolve("content/dist");
        return path -> {
            if (!path.startsWith(root)) return null;
            Path f = dist.resolve(path.substring(root.length()));
            return Files.exists(f) ? Files.readAllBytes(f) : null;
        };
    }
}
