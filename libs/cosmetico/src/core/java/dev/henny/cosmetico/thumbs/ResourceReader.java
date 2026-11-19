package dev.henny.cosmetico.thumbs;

import java.io.IOException;
import java.io.InputStream;

public interface ResourceReader {
    byte[] read(String path) throws IOException;

    static ResourceReader classpath(Class<?> anchor) {
        return path -> {
            try (InputStream in = anchor.getResourceAsStream(path)) {
                return in == null ? null : in.readAllBytes();
            }
        };
    }
}
