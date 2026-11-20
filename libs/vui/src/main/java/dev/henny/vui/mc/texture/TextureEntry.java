package dev.henny.vui.mc.texture;

import dev.henny.vui.render.TextureInfo;

public record TextureEntry(Object id, TextureInfo info) {
    public int width() {
        return info.width();
    }

    public int height() {
        return info.height();
    }
}
