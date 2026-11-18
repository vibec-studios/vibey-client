package dev.henny.vibeyclient.module.media.lyrics.render;

import dev.henny.vibeyclient.module.media.lyrics.LyricsCompat;
import dev.henny.vibeyclient.module.media.lyrics.LyricsConfig;
import dev.henny.vibeyclient.module.media.lyrics.Playback;
import dev.henny.vibeyclient.module.media.lyrics.T;
import dev.henny.vibeyclient.module.media.lyrics.lyrics.LrcParser;
import dev.henny.vibeyclient.module.media.lyrics.lyrics.Lyrics;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.hud.HudLayout;
import net.minecraft.network.chat.Component;

/**
 * Lyrics as a screen overlay (position "HUD"), a movable element of the HUD editor: drawn into a box of
 * {@link #BOX_W} × {@link #BOX_H} GUI pixels (times the element's scale), centred, or along the box's left/right side
 * when the element sits at a screen edge.
 */
public final class HudLyrics {
    public static final int BOX_W = 320;
    public static final int BOX_H = 110;
    /** Focal length of the fake perspective for 3D animations, in font pixels. */
    private static final float ROOM = 220f;
    /** Font pixels per GUI pixel at element scale 100 % and size 100 %. */
    private static final float BASE = 2f;
    private static Lyrics sample;

    private HudLyrics() {}

    public static void render(Gfx g, int w, int h, boolean preview) {
        LyricsConfig cfg = LyricsConfig.get();
        Lyrics lyrics = Playback.lyrics();
        String title = Playback.song() == null ? "" : Playback.song().title();
        String artist = Playback.song() == null ? "" : Playback.song().artist();
        long t = Playback.lyricTimeMs();
        if (preview && (Playback.song() == null || lyrics == null)) {
            lyrics = sample();
            title = T.get("hud.sample.title");
            artist = "";
            t = 1200 + (System.currentTimeMillis() % 7000L);
        } else if (Playback.song() == null || (!preview && LyricsCompat.overlayHidden())) {
            return;
        }
        float scale = BASE * cfg.size / 100f;
        float maxWidth = w * 0.96f / scale;
        LyricLayout.Frame frame = LyricLayout.build(g.font, lyrics, title, artist, t, maxWidth, ROOM);
        if (frame.isEmpty()) return;
        int align = dev.henny.vibeyclient.module.media.lyrics.LyricsFeature.instance() == null ? 0
                : HudLayout.alignX(dev.henny.vibeyclient.module.media.lyrics.LyricsFeature.instance().hudText);
        float cx = align < 0 ? Math.min(w / 2f, frame.width() * scale / 2f + 2) : align > 0 ? Math.max(w / 2f, w - frame.width() * scale / 2f - 2) : w / 2f;
        g.push();
        // the sung line a bit above the middle, so the next line below still fits into the box
        g.translate(cx, h * 0.4f);
        g.scale(scale, scale);
        new Painter(g.font) {
            @Override
            protected void draw(Component c, LyricLayout.Run r, float ox, float oy, float oz, int argb, boolean back) {
                // The screen has no depth: words further away get smaller, turned words narrower.
                float s = r.scale();
                float p = ROOM / Math.max(ROOM * 0.15f, ROOM - r.z());
                g.push();
                g.translate(r.x() * p, (r.y() + LyricLayout.ROW * s / 2f) * p);
                if (r.roll() != 0f) g.rotate((float) Math.toRadians(r.roll()));
                float squash = Math.max(0.2f, Math.abs((float) Math.cos(Math.toRadians(r.yaw()))));
                g.scale(s * p * squash, s * p);
                g.translate(-font.width(c) / 2f + ox, -LyricLayout.ROW / 2f + oy);
                g.text(c, 0, 0, argb);
                g.pop();
            }

            @Override
            protected void box(float x1, float y1, float x2, float y2, int argb) {
                g.push();
                g.translate(x1, y1);
                // fill() takes whole numbers: draw in tenths of a font pixel.
                g.scale(0.1f, 0.1f);
                g.fill(0, 0, Math.round((x2 - x1) * 10), Math.round((y2 - y1) * 10), argb);
                g.pop();
            }
        }.paint(frame);
        g.pop();
    }

    /** Two made-up lines for the HUD editor while nothing plays. */
    private static Lyrics sample() {
        if (sample == null) {
            sample = LrcParser.parse("[00:00.50]" + T.get("hud.sample.line1") + "\n[00:04.00]" + T.get("hud.sample.line2") + "\n[00:07.50]");
        }
        return sample;
    }

    /** Language changed: rebuild the sample. */
    public static void resetSample() {
        sample = null;
    }
}
