package dev.henny.vui.reload;

import java.nio.file.Path;

public interface ResourceRoot {
    String read(String relativePath);

    String describe(String relativePath);

    default Path directory() {
        return null;
    }
}
