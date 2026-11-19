package dev.henny.cosmetico.fixtures;

import dev.henny.cosmetico.thumbs.ThumbnailTextures;

import java.util.ArrayList;
import java.util.List;

public final class MemoryTextures implements ThumbnailTextures<MemoryTextures.Tex> {
    public static final class Tex {
        public final int bytes;
        public boolean released;

        Tex(int bytes) {
            this.bytes = bytes;
        }
    }

    public final List<Tex> uploaded = new ArrayList<>();

    @Override
    public Tex upload(byte[] png) {
        Tex t = new Tex(png.length);
        uploaded.add(t);
        return t;
    }

    @Override
    public void release(Tex texture) {
        texture.released = true;
    }

    @Override
    public boolean released(Tex texture) {
        return texture.released;
    }
}
