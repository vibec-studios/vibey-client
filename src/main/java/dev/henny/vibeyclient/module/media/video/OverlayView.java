package dev.henny.vibeyclient.module.media.video;

import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import net.minecraft.resources.Identifier;

public final class OverlayView {
    private static final int BACK = 0xE0101014;

    private final Video video;

    OverlayView(Video video) {
        this.video = video;
    }

    public void draw(Gfx g, int w, int h, boolean preview) {
        MediaScreen s = video.overlayScreen();
        if (s == null && !preview) return;
        g.rect(-1, -1, w + 2, h + 2, BACK);
        Identifier tex = s != null && s.player != null ? s.player.texture() : null;
        if (tex != null && !s.player.audioOnly()) {
            int tw = s.player.width();
            int th = s.player.height();
            g.image(tex, 0, 0, w, h, 0, 0, tw, th, tw, th);
            return;
        }
        String line = s == null ? Video.tr("hud.overlay") : video.status.line(s);
        if (line == null) line = Video.tr("status.loading");
        g.textCenter(g.clip(line, w - 8), w / 2, h / 2 - 4, Theme.MUTED);
    }
}
