package dev.henny.r3.render;

public interface TextureHandle {
    int width();

    int height();

    static TextureHandle resource(String id, int width, int height) {
        return new ResourceTexture(id, width, height);
    }
}
