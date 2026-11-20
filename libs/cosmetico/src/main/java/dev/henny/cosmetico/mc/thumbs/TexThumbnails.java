package dev.henny.cosmetico.mc.thumbs;

import dev.henny.cosmetico.mc.render.Tex;
import dev.henny.cosmetico.thumbs.ThumbnailTextures;

final class TexThumbnails implements ThumbnailTextures<Tex> {
    @Override
    public Tex upload(byte[] png) throws Exception {
        return Tex.upload("thumb", png);
    }

    @Override
    public void release(Tex texture) {
        texture.release();
    }

    @Override
    public boolean released(Tex texture) {
        return texture.released();
    }
}
