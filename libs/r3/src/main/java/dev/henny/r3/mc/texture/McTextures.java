package dev.henny.r3.mc.texture;

import dev.henny.r3.mc.compat.Game;
import dev.henny.r3.mc.compat.Ids;
import dev.henny.r3.render.ResourceTexture;
import dev.henny.r3.render.TextureHandle;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class McTextures {
    private static final int[] UNKNOWN = new int[0];
    private static final Map<String, int[]> SIZES = new HashMap<>();

    private McTextures() {}

    public static String id(TextureHandle handle) {
        if (handle == null) return null;
        if (handle instanceof McTexture t) return t.textureId();
        if (handle instanceof ResourceTexture r) return Ids.texturePath(r.id());
        return null;
    }

    public static TextureHandle resource(String id) {
        int[] size = size(id);
        return size == null ? null : TextureHandle.resource(id, size[0], size[1]);
    }

    public static int[] size(String id) {
        String path = Ids.texturePath(id);
        int[] known = SIZES.get(path);
        if (known == null) {
            known = read(path);
            SIZES.put(path, known);
        }
        return known == UNKNOWN ? null : known;
    }

    public static void forgetSizes() {
        SIZES.clear();
    }

    private static int[] read(String path) {
        var location = Ids.parse(path);
        if (location == null) return UNKNOWN;
        Optional<Resource> resource = Game.mc().getResourceManager().getResource(location);
        if (resource.isEmpty()) return UNKNOWN;
        try (InputStream in = resource.get().open()) {
            int[] size = PngSize.read(in);
            return size == null ? UNKNOWN : size;
        } catch (IOException e) {
            return UNKNOWN;
        }
    }
}
