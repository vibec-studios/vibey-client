package dev.henny.vui.reload;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LooseFileRoot implements ResourceRoot {
    public static final String NAMESPACE = "preview";
    private static final String PREFIX = "assets/" + NAMESPACE + "/vui/";

    private final Path dir;

    public LooseFileRoot(Path dir) {
        this.dir = dir.toAbsolutePath().normalize();
    }

    @Override
    public String read(String relativePath) {
        if (!relativePath.startsWith(PREFIX)) return null;
        Path p = dir.resolve(relativePath.substring(PREFIX.length()));
        if (!Files.isRegularFile(p)) return null;
        try {
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String describe(String relativePath) {
        return dir.resolve(relativePath.startsWith(PREFIX) ? relativePath.substring(PREFIX.length()) : relativePath).toString().replace('\\', '/');
    }

    @Override
    public Path directory() {
        return dir;
    }

    @Override
    public DocumentId idFor(Path file) {
        Path abs = file.toAbsolutePath().normalize();
        if (!abs.startsWith(dir)) return null;
        String rel = dir.relativize(abs).toString().replace('\\', '/');
        if (rel.endsWith(DocumentId.JSON)) return new DocumentId(NAMESPACE, rel.substring(0, rel.length() - DocumentId.JSON.length()));
        if (rel.endsWith(DocumentId.MARKUP)) return new DocumentId(NAMESPACE, rel.substring(0, rel.length() - DocumentId.MARKUP.length()));
        return null;
    }
}
