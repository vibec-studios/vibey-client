package dev.henny.vibeyclient.module.media.video;

import com.google.gson.JsonObject;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import net.minecraft.resources.Identifier;

/**
 * Picture-in-picture: the player of one screen drawn on the HUD. Movable/scalable through {@link HudElement};
 * the layout lives in the settings data under {@code "overlay"}. Nothing is drawn on top of the picture.
 */
public final class Overlay implements HudElement {
    private static final int BASE_WIDTH = 192;
    private final Video video;

    Overlay(Video video) {
        this.video = video;
    }

    @Override
    public String id() {
        return "video.overlay";
    }

    @Override
    public String nameKey() {
        return "video.overlay.name";
    }

    @Override
    public boolean visible() {
        return video.overlayScreen() != null;
    }

    @Override
    public int width() {
        return BASE_WIDTH;
    }

    @Override
    public int height() {
        MediaScreen s = video.overlayScreen();
        float a = s == null ? 16f / 9f : s.aspect();
        return Math.max(24, Math.round(BASE_WIDTH / a));
    }

    private JsonObject layout() {
        JsonObject d = video.cfg.group.data();
        JsonObject o = d.getAsJsonObject("overlay");
        if (o == null) {
            o = new JsonObject();
            o.addProperty("x", 0.98f);
            o.addProperty("y", 0.02f);
            o.addProperty("scale", 1f);
            d.add("overlay", o);
        }
        return o;
    }

    @Override
    public float x() {
        return layout().get("x").getAsFloat();
    }

    @Override
    public float y() {
        return layout().get("y").getAsFloat();
    }

    @Override
    public float scale() {
        return layout().get("scale").getAsFloat();
    }

    @Override
    public void setLayout(float x, float y, float scale) {
        JsonObject o = layout();
        o.addProperty("x", Math.max(0f, Math.min(1f, x)));
        o.addProperty("y", Math.max(0f, Math.min(1f, y)));
        o.addProperty("scale", Math.max(minScale(), Math.min(maxScale(), scale)));
        video.cfg.group.markDirty();
    }

    /** Top-left pixel position for the stored layout (x/y are clamped so the whole picture stays on screen). */
    public int[] position(Gfx g) {
        float s = scale();
        int w = Math.round(width() * s);
        int h = Math.round(height() * s);
        int px = Math.round(x() * Math.max(0, g.screenWidth() - w));
        int py = Math.round(y() * Math.max(0, g.screenHeight() - h));
        return new int[] {px, py, w, h};
    }

    void renderAtLayout(Gfx g) {
        if (!visible()) return;
        int[] p = position(g);
        render(g, p[0], p[1], scale(), false);
    }

    @Override
    public void render(Gfx g, int x, int y, float scale, boolean editing) {
        int w = Math.round(width() * scale);
        int h = Math.round(height() * scale);
        MediaScreen s = video.overlayScreen();
        g.rect(x - 1, y - 1, w + 2, h + 2, 0xE0101014);
        Identifier tex = s != null && s.player != null ? s.player.texture() : null;
        if (tex != null && !s.player.audioOnly()) {
            int tw = s.player.width();
            int th = s.player.height();
            g.image(tex, x, y, w, h, 0, 0, tw, th, tw, th);
        } else {
            String line = s == null ? Video.tr("overlay.name") : video.statusLine(s);
            if (line == null) line = Video.tr("status.loading");
            g.textCenter(g.clip(line, w - 8), x + w / 2, y + h / 2 - 4, Theme.MUTED);
        }
        if (editing) g.outline(x - 1, y - 1, w + 2, h + 2, Theme.BRAND);
    }
}
