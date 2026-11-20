package dev.henny.vui.headless;

import dev.henny.vui.render.TextureInfo;
import dev.henny.vui.render.Textures;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HeadlessTextures implements Textures {
    private final Map<String, BufferedImage> images = new HashMap<>();
    private final Map<String, TextureInfo> sizes = new HashMap<>();
    private final List<Path> roots = new ArrayList<>();
    private final TintedImages tinted = new TintedImages();

    public HeadlessTextures root(Path assetsRoot) {
        roots.clear();
        if (assetsRoot != null) roots.add(assetsRoot);
        return this;
    }

    public HeadlessTextures addRoot(Path assetsRoot) {
        roots.add(assetsRoot);
        return this;
    }

    public BufferedImage tinted(BufferedImage image, int argb) {
        return tinted.get(image, argb);
    }

    public HeadlessTextures put(String id, BufferedImage image) {
        BufferedImage old = images.put(id, image);
        if (old != null) tinted.clear();
        sizes.put(id, new TextureInfo(image.getWidth(), image.getHeight()));
        return this;
    }

    public HeadlessTextures size(String id, int width, int height) {
        sizes.put(id, new TextureInfo(width, height));
        return this;
    }

    @Override
    public TextureInfo info(String id) {
        TextureInfo known = sizes.get(id);
        if (known != null) return known;
        BufferedImage image = image(id);
        return image == null ? null : sizes.get(id);
    }

    public BufferedImage image(String id) {
        BufferedImage cached = images.get(id);
        if (cached != null || roots.isEmpty()) return cached;
        Path file = null;
        for (Path root : roots) {
            Path candidate = file(root, id);
            if (Files.isRegularFile(candidate)) {
                file = candidate;
                break;
            }
        }
        if (file == null) return null;
        try {
            BufferedImage loaded = javax.imageio.ImageIO.read(file.toFile());
            if (loaded != null) put(id, loaded);
            return loaded;
        } catch (IOException e) {
            return null;
        }
    }

    private static Path file(Path root, String id) {
        int colon = id.indexOf(':');
        if (colon < 0) return root.resolve(id);
        String path = id.substring(colon + 1);
        if (!path.endsWith(".png")) path = path + ".png";
        return root.resolve("assets").resolve(id.substring(0, colon)).resolve("textures").resolve(path);
    }
}
