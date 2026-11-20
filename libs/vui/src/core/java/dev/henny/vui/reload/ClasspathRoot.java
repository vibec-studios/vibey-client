package dev.henny.vui.reload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class ClasspathRoot implements ResourceRoot {
    private final ClassLoader loader;

    public ClasspathRoot(ClassLoader loader) {
        this.loader = loader;
    }

    @Override
    public String read(String relativePath) {
        try (InputStream in = loader.getResourceAsStream(relativePath)) {
            if (in == null) return null;
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String describe(String relativePath) {
        return relativePath;
    }
}
