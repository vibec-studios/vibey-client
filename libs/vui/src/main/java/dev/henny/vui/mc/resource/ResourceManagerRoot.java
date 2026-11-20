package dev.henny.vui.mc.resource;

import dev.henny.vui.mc.compat.Ids;
import dev.henny.vui.reload.ResourceRoot;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ResourceManagerRoot implements ResourceRoot {
    private static final String ASSETS = "assets/";

    private final Map<String, String> packs = new ConcurrentHashMap<>();

    @Override
    public String read(String relativePath) {
        Optional<Resource> resource = find(relativePath);
        if (resource.isEmpty()) return null;
        try (InputStream in = resource.get().open()) {
            packs.put(relativePath, resource.get().sourcePackId());
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String describe(String relativePath) {
        String pack = packs.get(relativePath);
        return pack == null ? relativePath : pack + "/" + relativePath;
    }

    private static Optional<Resource> find(String relativePath) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || !relativePath.startsWith(ASSETS)) return Optional.empty();
        ResourceManager manager = mc.getResourceManager();
        if (manager == null) return Optional.empty();
        String rest = relativePath.substring(ASSETS.length());
        int slash = rest.indexOf('/');
        if (slash <= 0) return Optional.empty();
        String namespace = rest.substring(0, slash);
        String path = rest.substring(slash + 1);
        if (!Ids.valid(namespace, path)) return Optional.empty();
        return manager.getResource(Ids.of(namespace, path));
    }
}
