package dev.henny.cosmetico.thumbs;

public interface ThumbnailTextures<T> {
    T upload(byte[] png) throws Exception;

    void release(T texture);

    boolean released(T texture);
}
