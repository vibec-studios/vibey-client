package dev.henny.vibeyclient.module.media.lyrics.render;

import dev.henny.vibeyclient.module.media.lyrics.T;
import dev.henny.vibeyclient.module.media.lyrics.LyricsCompat;
import dev.henny.vibeyclient.module.media.lyrics.LyricsConfig;
import dev.henny.vibeyclient.module.media.lyrics.Playback;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.module.media.lyrics.library.Song;
import dev.henny.vibeyclient.module.media.lyrics.lyrics.Lyrics;
import dev.henny.vibeyclient.module.media.lyrics.sky.SkyLook;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The small bar at the top of the screen: the album art (or a note), the line that is sung right now (words light up
 * as they are sung), a little equalizer and the song's progress. A movable element of the HUD editor: the bar is
 * drawn into a box of {@link #BOX_W} × {@link #H} (centred, or along the side the element is anchored to).
 */
public final class NowPlayingBar {
    public static final int H = 17;
    /** The widest the bar gets (its box in the HUD editor). */
    public static final int BOX_W = 260;
    private static final int BG = 0xD8121218;
    private static final int EDGE = 0x30FFFFFF;
    private static final int ACCENT = 0xFFD946EF;
    private static final int DIM = 0xFF8E8E9E;

    private static final int OK = 0xFF34D399;
    /** The song the bar showed last and when it changed (for the little "next song" intro). */
    private static String songKey = "";
    private static long songSince;
    /** The highlight chip glides from word to word. */
    private static float chipX = Float.NaN;
    private static float chipW;

    private NowPlayingBar() {}

    public static void render(Gfx g, int boxW, int boxH, boolean preview) {
        LyricsConfig cfg = LyricsConfig.get();
        Song song = Playback.song();
        if (!preview && (!cfg.pill || song == null || LyricsCompat.overlayHidden())) return;
        if (song == null) {
            sample(g, boxW);
            return;
        }
        int[] themed = SkyLook.themed(cfg);
        int high = themed != null ? themed[1] : cfg.highlightColor;
        high |= 0xFF000000;
        int[] art = Cover.colors();
        int accent = art != null ? art[1] | 0xFF000000 : ACCENT;
        int accent2 = art != null ? LyricLayout.mix(accent, 0xFFFFFFFF, 0.35f) : 0xFFB98CF5;

        long now = System.currentTimeMillis();
        String key = song.id() + "|" + song.title();
        if (!key.equals(songKey)) {
            songKey = key;
            songSince = now;
            chipX = Float.NaN;
        }
        long t = Playback.lyricTimeMs();
        Lyrics lyrics = Playback.lyrics();
        Lyrics.Line line = null;
        if (lyrics != null && !lyrics.lines().isEmpty()) {
            int idx = lyrics.lineAt(t);
            if (idx >= 0 && !lyrics.lines().get(idx).isBreak() && t < lyrics.lines().get(idx).end() + 2500) line = lyrics.lines().get(idx);
        }

        int maxW = boxW;
        int textW;
        String fallback = null;
        // A new song first says "next song", then shows its title until the first line is sung.
        boolean intro = now - songSince < 900;
        if (line != null && !intro) {
            textW = g.width(lineText(line.words()));
        } else {
            fallback = intro ? T.get("bar.next")
                    : "♪ " + (song.artist().isBlank() ? song.title() : song.title() + " · " + song.artist());
            textW = g.width(fallback) + 12;
        }
        int w = Math.max(150, Math.min(maxW, textW + 52));
        int align = align();
        int x = align < 0 ? 0 : align > 0 ? boxW - w : (boxW - w) / 2;
        int y = 0;

        // pill: a box with its corners cut off
        g.fill(x + 2, y, x + w - 2, y + H, BG);
        g.fill(x, y + 2, x + 2, y + H - 2, BG);
        g.fill(x + w - 2, y + 2, x + w, y + H - 2, BG);
        g.fill(x + 1, y + 1, x + 2, y + 2, BG);
        g.fill(x + w - 2, y + 1, x + w - 1, y + 2, BG);
        g.fill(x + 1, y + H - 2, x + 2, y + H - 1, BG);
        g.fill(x + w - 2, y + H - 2, x + w - 1, y + H - 1, BG);
        g.fill(x + 2, y, x + w - 2, y + 1, EDGE);

        // album art, or a note in a little disc
        var cover = Cover.texture();
        if (cover != null) {
            g.image(cover, x + 4, y + 2, 13, 13, Cover.SIZE);
        } else {
            g.fill(x + 5, y + 3, x + 16, y + 14, 0xFF2A1F33);
            g.text(Component.literal("♪"), x + 7, y + 5, ACCENT);
        }

        // song time, left of the bar (right of it when the bar sits at the left edge)
        if (cfg.clock) {
            String clock = clock(Playback.positionMs());
            int cx = align < 0 ? x + w + 4 : x - 4 - g.width(clock);
            g.text(clock, cx + 1, y + 6, 0x90000000);
            g.text(clock, cx, y + 5, 0xFFF2F2F7);
        }

        // equalizer
        boolean playing = Playback.isPlaying();
        int ex = x + w - 18;
        for (int i = 0; i < 4; i++) {
            float k = playing ? (float) Math.abs(Math.sin(now / 160.0 + i * 1.7) * Math.cos(now / 410.0 + i)) : 0.15f;
            int bh = 2 + Math.round(7 * k);
            g.fill(ex + i * 3, y + 12 - bh, ex + i * 3 + 2, y + 12, i % 2 == 0 ? accent : accent2);
        }

        // the line, scrolled so the word being sung stays in view
        int tx = x + 21;
        int avail = ex - 5 - tx;
        g.scissor(tx, y, tx + avail, y + H);
        if (fallback != null) {
            // green check, then the text
            g.fill(tx + 1, y + 4, tx + 8, y + 12, OK);
            g.fill(tx, y + 5, tx + 9, y + 11, OK);
            g.fill(tx + 2, y + 8, tx + 3, y + 9, 0xFF0B3B2A);
            g.fill(tx + 3, y + 9, tx + 4, y + 10, 0xFF0B3B2A);
            g.fill(tx + 4, y + 8, tx + 5, y + 9, 0xFF0B3B2A);
            g.fill(tx + 5, y + 7, tx + 6, y + 8, 0xFF0B3B2A);
            g.fill(tx + 6, y + 6, tx + 7, y + 7, 0xFF0B3B2A);
            g.text(g.clip(fallback, avail - 12), tx + 12, y + 5, 0xFFE8E8F0);
        } else {
            List<Lyrics.Word> words = line.words();
            int curEnd = 0;
            int pos = 0;
            for (Lyrics.Word word : words) {
                pos += g.width(word.text() + " ");
                if (t >= word.start()) curEnd = pos;
            }
            int shift = Math.max(0, Math.min(textW - avail, curEnd - avail + 8));
            if (textW <= avail) shift = -(avail - textW) / 2;
            int wx = tx - shift;
            // a colored chip sits behind the word being sung and glides to the next one
            int target = -1;
            int targetW = 0;
            int px = wx;
            for (Lyrics.Word word : words) {
                if (t >= word.start() && t < Math.max(word.end(), word.start() + 150) + 120) {
                    target = px;
                    targetW = g.width(word.text());
                }
                px += g.width(word.text() + " ");
            }
            if (target >= 0) {
                if (Float.isNaN(chipX)) {
                    chipX = target;
                    chipW = targetW;
                }
                chipX += (target - chipX) * 0.35f;
                chipW += (targetW - chipW) * 0.35f;
                int cx1 = Math.round(chipX) - 3;
                int cx2 = Math.round(chipX + chipW) + 3;
                g.fill(cx1 + 1, y + 3, cx2 - 1, y + 14, accent);
                g.fill(cx1, y + 4, cx2, y + 13, accent);
            }
            for (Lyrics.Word word : words) {
                int color;
                if (t < word.start()) color = DIM;
                else if (t < word.end() + 120) color = target >= 0 ? 0xFFFFFFFF : high;
                else color = 0xFFE8E8F0;
                g.text(Component.literal(word.text()), wx, y + 5, color);
                wx += g.width(word.text() + " ");
            }
        }
        g.endScissor();

        // progress
        long dur = Playback.durationMs();
        if (dur > 0) {
            int pw = Math.round((w - 8) * Math.min(1f, Playback.positionMs() / (float) dur));
            g.fill(x + 4, y + H - 2, x + w - 4, y + H - 1, 0x30FFFFFF);
            g.fill(x + 4, y + H - 2, x + 4 + pw, y + H - 1, accent);
        }
    }

    private static int align() {
        var f = dev.henny.vibeyclient.module.media.lyrics.LyricsFeature.instance();
        return f == null ? 0 : dev.henny.vibeyclient.ui.hud.HudLayout.alignX(f.hudBar);
    }

    /** The HUD editor while nothing plays: an example bar. */
    private static void sample(Gfx g, int boxW) {
        String text = "♪ " + T.get("hud.sample.title");
        int w = Math.max(150, Math.min(boxW, g.width(text) + 52));
        int align = align();
        int x = align < 0 ? 0 : align > 0 ? boxW - w : (boxW - w) / 2;
        g.fill(x + 2, 0, x + w - 2, H, BG);
        g.fill(x, 2, x + w, H - 2, BG);
        g.fill(x + 2, 0, x + w - 2, 1, EDGE);
        g.fill(x + 5, 3, x + 16, 14, 0xFF2A1F33);
        g.text(Component.literal("♪"), x + 7, 5, ACCENT);
        g.text(g.clip(text, w - 50), x + 21, 5, 0xFFE8E8F0);
        long now = System.currentTimeMillis();
        int ex = x + w - 18;
        for (int i = 0; i < 4; i++) {
            float k = (float) Math.abs(Math.sin(now / 160.0 + i * 1.7) * Math.cos(now / 410.0 + i));
            int bh = 2 + Math.round(7 * k);
            g.fill(ex + i * 3, 12 - bh, ex + i * 3 + 2, 12, i % 2 == 0 ? ACCENT : 0xFFB98CF5);
        }
        g.fill(x + 4, H - 2, x + w - 4, H - 1, 0x30FFFFFF);
        g.fill(x + 4, H - 2, x + 4 + (w - 8) / 3, H - 1, ACCENT);
    }

    private static String clock(long ms) {
        long sec = Math.max(0, ms) / 1000;
        return String.format(java.util.Locale.ROOT, "%d:%02d", sec / 60, sec % 60);
    }

    private static String lineText(List<Lyrics.Word> words) {
        StringBuilder sb = new StringBuilder();
        for (Lyrics.Word w : words) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(w.text());
        }
        return sb.toString();
    }
}
