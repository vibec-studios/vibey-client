package dev.henny.vui.mc.texture;

import dev.henny.vui.render.TextureInfo;
import dev.henny.vui.render.Textures;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class McTextures implements Textures {
    private final Map<String, TextureEntry> loaded = new HashMap<>();
    private final Map<String, TextureEntry> registered = new HashMap<>();
    private final Set<String> missing = new HashSet<>();

    @Override
    public TextureInfo info(String id) {
        TextureEntry e = entry(id);
        return e == null ? null : e.info();
    }

    public TextureEntry entry(String id) {
        if (id == null || id.isEmpty()) return null;
        TextureEntry e = registered.get(id);
        if (e != null) return e;
        e = loaded.get(id);
        if (e != null || missing.contains(id)) return e;
        e = TextureFiles.load(id);
        if (e == null) missing.add(id);
        else loaded.put(id, e);
        return e;
    }

    public void register(String id, Object location, int width, int height) {
        registered.put(id, new TextureEntry(location, new TextureInfo(width, height)));
    }

    public void unregister(String id) {
        registered.remove(id);
    }

    public void clear() {
        loaded.clear();
        missing.clear();
    }
}
