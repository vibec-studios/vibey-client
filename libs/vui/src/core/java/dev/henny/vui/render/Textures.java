package dev.henny.vui.render;

public interface Textures {
    Textures NONE = id -> null;

    TextureInfo info(String id);

    default boolean exists(String id) {
        return info(id) != null;
    }
}
