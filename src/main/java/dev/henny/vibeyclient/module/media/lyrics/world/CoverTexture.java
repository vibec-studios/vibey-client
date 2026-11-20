package dev.henny.vibeyclient.module.media.lyrics.world;

import dev.henny.r3.mc.texture.McTexture;
import dev.henny.r3.render.TextureHandle;
import dev.henny.vibeyclient.module.media.lyrics.render.Cover;

final class CoverTexture implements TextureHandle, McTexture {
    static final String ID = "vibeyclient:music/cover";

    @Override
    public String textureId() {
        var id = Cover.texture();
        return id == null ? null : id.toString();
    }

    @Override
    public int width() {
        return Cover.SIZE;
    }

    @Override
    public int height() {
        return Cover.SIZE;
    }
}
