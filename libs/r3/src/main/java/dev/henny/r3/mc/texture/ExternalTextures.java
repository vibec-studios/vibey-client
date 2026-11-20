package dev.henny.r3.mc.texture;

import dev.henny.r3.render.TextureHandle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ExternalTextures {
    private static final Map<String, TextureHandle> HANDLES = new ConcurrentHashMap<>();

    private ExternalTextures() {}

    public static void register(String id, TextureHandle handle) {
        if (handle == null) HANDLES.remove(id);
        else HANDLES.put(id, handle);
    }

    public static TextureHandle get(String id) {
        return id == null ? null : HANDLES.get(id);
    }
}
