package dev.henny.vui.reload;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DirectoryRoot implements ResourceRoot {
    private final Path dir;

    public DirectoryRoot(Path dir) {
        this.dir = dir.toAbsolutePath().normalize();
    }

    @Override
    public String read(String relativePath) {
        Path p = dir.resolve(relativePath);
        if (!Files.isRegularFile(p)) return null;
        try {
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String describe(String relativePath) {
        return dir.resolve(relativePath).toString().replace('\\', '/');
    }

    @Override
    public Path directory() {
        return dir;
    }

    public String relativize(Path file) {
        return dir.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }
}
