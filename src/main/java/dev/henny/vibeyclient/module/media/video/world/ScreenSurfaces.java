package dev.henny.vibeyclient.module.media.video.world;

import dev.henny.r3.object.VideoSurface;
import dev.henny.vibeyclient.media.player.MediaPlayer;
import dev.henny.vibeyclient.module.media.video.MediaScreen;
import dev.henny.vibeyclient.module.media.video.Video;
import dev.henny.vibeyclient.module.media.video.controls.ScreenControls;

public final class ScreenSurfaces {
    private final Video video;

    public ScreenSurfaces(Video video) {
        this.video = video;
    }

    public void tick() {
        for (MediaScreen s : video.screens.values()) {
            if (!s.placed()) continue;
            VideoSurface surface = ScreenObjects.surface(s.object);
            if (surface == null) continue;
            surface.texture(picture(s, surface));
            surface.status(surface.showsPicture() ? null : video.status.line(s));
            s.object.fullBright(s.glowing);
            ScreenObjects.fitLayout(s.object);
            ScreenControls c = ScreenControls.of(s);
            if (c != null) c.refresh();
        }
    }

    private static PlayerTexture picture(MediaScreen s, VideoSurface surface) {
        MediaPlayer p = s.player;
        if (p == null || p.audioOnly() || p.texture() == null || p.status() == MediaPlayer.Status.ERROR) return null;
        if (surface.texture() instanceof PlayerTexture t && t.player() == p) return t;
        return new PlayerTexture(p);
    }
}
