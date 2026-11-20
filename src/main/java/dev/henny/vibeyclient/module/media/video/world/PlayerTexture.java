package dev.henny.vibeyclient.module.media.video.world;

import dev.henny.r3.mc.texture.McTexture;
import dev.henny.r3.render.TextureHandle;
import dev.henny.vibeyclient.media.player.MediaPlayer;

public final class PlayerTexture implements TextureHandle, McTexture {
    private final MediaPlayer player;

    public PlayerTexture(MediaPlayer player) {
        this.player = player;
    }

    public MediaPlayer player() {
        return player;
    }

    @Override
    public String textureId() {
        var id = player.texture();
        return id == null ? null : id.toString();
    }

    @Override
    public int width() {
        return Math.max(1, player.width());
    }

    @Override
    public int height() {
        return Math.max(1, player.height());
    }
}
