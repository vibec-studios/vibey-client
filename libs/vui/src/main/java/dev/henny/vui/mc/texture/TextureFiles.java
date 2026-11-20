package dev.henny.vui.mc.texture;

import dev.henny.vui.mc.compat.Ids;
import dev.henny.vui.render.TextureInfo;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.InputStream;

final class TextureFiles {
    private TextureFiles() {
    }

    static TextureEntry load(String id) {
        int colon = id.indexOf(':');
        String namespace = colon < 0 ? "minecraft" : id.substring(0, colon);
        String path = location(colon < 0 ? id : id.substring(colon + 1));
        if (!Ids.valid(namespace, path)) return null;
        var location = Ids.of(namespace, path);
        var resource = Minecraft.getInstance().getResourceManager().getResource(location);
        if (resource.isEmpty()) return null;
        try (InputStream in = resource.get().open()) {
            int[] size = PngSize.read(in);
            return size == null ? null : new TextureEntry(location, new TextureInfo(size[0], size[1]));
        } catch (IOException e) {
            return null;
        }
    }

    static String location(String path) {
        String p = path.startsWith("textures/") ? path : "textures/" + path;
        return p.endsWith(".png") ? p : p + ".png";
    }
}
