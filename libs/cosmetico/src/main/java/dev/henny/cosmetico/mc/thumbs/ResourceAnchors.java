package dev.henny.cosmetico.mc.thumbs;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ResourceAnchors {
    private ResourceAnchors() {}

    private static final List<Class<?>> ANCHORS = new CopyOnWriteArrayList<>(List.of(ResourceAnchors.class));

    public static void add(Class<?> owner) {
        if (owner != null && !ANCHORS.contains(owner)) ANCHORS.add(0, owner);
    }

    public static byte[] read(String path) throws IOException {
        for (Class<?> c : ANCHORS) {
            try (InputStream in = c.getResourceAsStream(path)) {
                if (in != null) return in.readAllBytes();
            }
        }
        return null;
    }
}
