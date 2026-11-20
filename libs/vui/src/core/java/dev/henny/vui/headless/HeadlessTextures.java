package dev.henny.vui.headless;

import dev.henny.vui.render.TextureInfo;
import dev.henny.vui.render.Textures;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class HeadlessTextures implements Textures {
    private final Map<String, BufferedImage> images = new HashMap<>();
    private final Map<String, TextureInfo> sizes = new HashMap<>();
    private Path root;

    public HeadlessTextures root(Path assetsRoot) {
        this.root = assetsRoot;
        return this;
    }

    public HeadlessTextures put(String id, BufferedImage image) {
        images.put(id, image);
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
        if (cached != null || root == null) return cached;
        Path file = file(id);
        if (file == null || !Files.isRegularFile(file)) return null;
        try {
            BufferedImage loaded = javax.imageio.ImageIO.read(file.toFile());
            if (loaded != null) put(id, loaded);
            return loaded;
        } catch (IOException e) {
            return null;
        }
    }

    private Path file(String id) {
        int colon = id.indexOf(':');
        if (colon < 0) return root.resolve(id);
        String path = id.substring(colon + 1);
        if (!path.endsWith(".png")) path = path + ".png";
        return root.resolve("assets").resolve(id.substring(0, colon)).resolve("textures").resolve(path);
    }
}
