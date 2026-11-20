package dev.henny.r3.mc.vui;

import dev.henny.r3.mc.texture.ExternalTextures;
import dev.henny.r3.mc.texture.McTextures;
import dev.henny.vui.render.TextureInfo;
import dev.henny.vui.render.Textures;

final class McVuiTextures implements Textures {
    @Override
    public TextureInfo info(String id) {
        var external = ExternalTextures.get(id);
        if (external != null) return new TextureInfo(external.width(), external.height());
        int[] size = McTextures.size(id);
        return size == null ? null : new TextureInfo(size[0], size[1]);
    }
}
