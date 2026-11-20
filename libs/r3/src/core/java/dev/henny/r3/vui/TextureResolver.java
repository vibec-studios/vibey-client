package dev.henny.r3.vui;

import dev.henny.r3.render.TextureHandle;
import dev.henny.vui.render.TextureInfo;
import dev.henny.vui.render.Textures;

@FunctionalInterface
public interface TextureResolver {
    TextureResolver NONE = id -> null;

    TextureHandle resolve(String id);

    static TextureResolver resources(Textures textures) {
        return id -> {
            TextureInfo info = textures.info(id);
            return info == null ? null : TextureHandle.resource(id, info.width(), info.height());
        };
    }
}
