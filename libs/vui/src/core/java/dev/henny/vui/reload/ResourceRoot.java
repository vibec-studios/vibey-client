package dev.henny.vui.reload;

import java.nio.file.Path;

public interface ResourceRoot {
    String read(String relativePath);

    String describe(String relativePath);

    default Path directory() {
        return null;
    }

    default String relativePath(Path file) {
        Path dir = directory();
        if (dir == null) return null;
        Path abs = file.toAbsolutePath().normalize();
        return abs.startsWith(dir) ? dir.relativize(abs).toString().replace(java.io.File.separatorChar, '/') : null;
    }

    default DocumentId idFor(Path file) {
        Path dir = directory();
        if (dir == null) return null;
        Path abs = file.toAbsolutePath().normalize();
        if (!abs.startsWith(dir)) return null;
        return DocumentId.fromResource(dir.relativize(abs).toString().replace('\\', '/'));
    }
}
