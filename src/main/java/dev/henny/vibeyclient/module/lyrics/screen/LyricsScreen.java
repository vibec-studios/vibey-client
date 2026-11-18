package dev.henny.vibeyclient.module.lyrics.screen;

import dev.henny.vibeyclient.module.lyrics.LyricsCompat;
import dev.henny.vibeyclient.module.lyrics.LyricsConfig;
import dev.henny.vibeyclient.module.lyrics.Playback;
import dev.henny.vibeyclient.module.lyrics.LyricsModule;
import dev.henny.vibeyclient.ui.CardScreen;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.module.lyrics.library.Song;
import dev.henny.vibeyclient.module.lyrics.library.SongLibrary;
import dev.henny.vibeyclient.module.lyrics.pc.NowPlaying;
import dev.henny.vibeyclient.module.lyrics.pc.PcLyrics;
import dev.henny.vibeyclient.module.lyrics.render.LyricLayout;
import dev.henny.vibeyclient.module.lyrics.sky.SkyTheme;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import dev.henny.vibeyclient.module.lyrics.T;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

/**
 * The main menu, kept small: what is playing, the song list, where the lyrics show up and one of a few looks.
 * Fonts, colors, sizes and sync live in {@link AdvancedScreen}. In a world the card sits on the left so the lyrics
 * stay visible while you change things.
 */
public final class LyricsScreen extends CardScreen {
    public LyricsScreen(Screen parent) {
        super(T.text("title"), parent, "lyrics.");
        SongLibrary.rescan();
    }

    @Override
    public void removed() {
        LyricsConfig.get().save();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean inWorld() {
        return minecraft != null && minecraft.level != null;
    }

    @Override
    protected int dimColor() {
        return inWorld() ? 0x40000000 : DIM_BG;
    }

    @Override
    protected int cardX(int cardWidth) {
        return inWorld() && width >= cardWidth * 2 ? 16 : super.cardX(cardWidth);
    }

    @Override
    protected int cardWidth() {
        return 300;
    }

    @Override
    protected int headerHeight() {
        return 34;
    }

    @Override
    protected int footerHeight() {
        return 38;
    }

    @Override
    protected void drawHeader(Gfx g, int x, int y, int w) {
        g.text(Component.literal("♪ ").withStyle(ChatFormatting.LIGHT_PURPLE), x + PAD, y + 13, ACCENT);
        title(g, T.get("title"), x + PAD + 10, y + 13);
        closeButton(g, x, y, w);
    }

    @Override
    protected int drawBody(Gfx g, int x, int y, int w) {
        y = nowPlaying(g, x, y, w);
        y += 12;
        y = songs(g, x, y, w);
        y += 12;

        y = section(g, T.get("position"), x, y);
        LyricsConfig cfg = LyricsConfig.get();
        LyricsConfig.Position[] positions = LyricsConfig.Position.values();
        String[] labels = new String[positions.length];
        for (int i = 0; i < positions.length; i++) labels[i] = T.get("position." + positions[i].name().toLowerCase(Locale.ROOT));
        y = segmented(g, x, y, w, labels, cfg.position.ordinal(), i -> {
            cfg.position = positions[i];
            if (cfg.position == LyricsConfig.Position.WORLD && !cfg.anchorSet) placeHere();
            cfg.save();
        });
        String hint = T.get("position." + cfg.position.name().toLowerCase(Locale.ROOT) + ".hint");
        y += 5;
        if (cfg.position == LyricsConfig.Position.WORLD || cfg.position == LyricsConfig.Position.SKY) {
            String label = T.get(cfg.position == LyricsConfig.Position.WORLD ? "place" : "aim");
            int bw = g.width(label) + 14;
            boolean can = inWorld();
            button(g, x + w - bw, y, bw, 16, label, can ? Style.SECONDARY : Style.DISABLED, () -> {
                if (cfg.position == LyricsConfig.Position.WORLD) placeHere();
                else aimSky();
            });
            g.text(g.clip(hint, w - bw - 8), x, y + 4, DIM);
            y += 16;
        } else {
            g.text(g.clip(hint, w), x, y, DIM);
            y += 10;
        }
        y += 10;

        y = section(g, T.get("style"), x, y);
        LyricsConfig.Preset[] presets = LyricsConfig.Preset.values();
        String[] names = new String[presets.length];
        for (int i = 0; i < presets.length; i++) names[i] = T.get("preset." + presets[i].name().toLowerCase(Locale.ROOT));
        for (int row = 0; row * 4 < presets.length; row++) {
            int from = row * 4;
            int n = Math.min(4, presets.length - from);
            String[] part = java.util.Arrays.copyOfRange(names, from, from + n);
            int sel = cfg.preset.ordinal() - from;
            y = segmented(g, x, y, w, part, sel >= 0 && sel < n ? sel : -1, i -> {
                cfg.applyPreset(presets[from + i]);
                cfg.save();
            }) + 2;
        }
        y += 10;
        y = skies(g, x, y, w);
        return y + 2;
    }

    /** Custom skies: a grid of picture tiles (a preview of each sky with its name). */
    private int skies(Gfx g, int x, int y, int w) {
        y = section(g, T.get("sky"), x, y);
        LyricsConfig cfg = LyricsConfig.get();
        SkyTheme[] skies = SkyTheme.values();
        int cols = 3;
        int gap = 3;
        int cw0 = (w - gap * (cols - 1)) / cols;
        int ch = Math.max(30, cw0 / 3 + 10);
        for (int i = 0; i < skies.length; i++) {
            SkyTheme s = skies[i];
            int cw = cw0;
            int cx = x + (i % cols) * (cw + gap);
            int cy = y + (i / cols) * (ch + gap);
            if (i % cols == cols - 1) cw = x + w - cx;
            boolean sel = s == SkyTheme.of(cfg.sky);
            boolean hov = hover(cx, cy, cw, ch);
            int ih = ch - 10;
            if (s == SkyTheme.VANILLA) {
                for (int k = 0; k < ih; k++) {
                    g.rect(cx, cy + k, cw, 1, LyricLayout.mix(0xFF6E9BEF, 0xFFC4DCFF, k / (float) (ih - 1)));
                }
            } else {
                // the thumbnail is 3:1; show its middle part when the tile is narrower
                float aspect = cw / (float) ih;
                int uw = Math.min(192, Math.round(64 * aspect));
                g.image(LyricsCompat.id("vibeyclient", "textures/lyrics/sky/thumb/" + s.id() + ".png"),
                        cx, cy, cw, ih, (192 - uw) / 2f, 0, uw, 64, 192, 64);
            }
            g.rect(cx, cy + ih, cw, ch - ih, sel ? 0xFF2A1F33 : (hov ? SURFACE_HOVER : SURFACE));
            if (hov && !sel) g.rect(cx, cy, cw, ih, 0x22FFFFFF);
            if (sel) {
                g.outline(cx, cy, cw, ch, ACCENT);
                g.outline(cx + 1, cy + 1, cw - 2, ch - 2, ACCENT);
            }
            String name = T.get("sky." + s.id());
            g.text(g.clip(name, cw - 6), cx + 3, cy + ih + 1, sel ? TEXT : MUTED);
            hit(cx, cy, cw, ch, () -> {
                cfg.sky = s.id();
                cfg.save();
            });
        }
        y += ((skies.length + cols - 1) / cols) * (ch + gap) + 3;
        g.text(g.clip(T.get("sky.hint"), w), x, y, DIM);
        return y + 10;
    }

    private int nowPlaying(Gfx g, int x, int y, int w) {
        Song song = Playback.song();
        int h = 54;
        g.rect(x, y, w, h, SURFACE);
        int bs = 26;
        int nx = x + w - 8 - 20;
        int bx = nx - 4 - bs;
        int by = y + 8;
        boolean pc = Playback.isPc();
        boolean skips = pc || song != null || !SongLibrary.songs().isEmpty();
        if (!skips) bx = x + w - 8 - bs;

        // Play / pause
        boolean hov = hover(bx, by, bs, bs);
        g.rect(bx, by, bs, bs, hov ? ACCENT_HOVER : ACCENT);
        if (Playback.isPlaying()) {
            g.rect(bx + 9, by + 8, 3, 10, 0xFFFFFFFF);
            g.rect(bx + 14, by + 8, 3, 10, 0xFFFFFFFF);
        } else {
            for (int i = 0; i < 6; i++) g.rect(bx + 10 + i, by + 7 + i, 1, 12 - i * 2, 0xFFFFFFFF);
        }
        hit(bx, by, bs, bs, Playback::toggle);

        // Previous (or back to the start) and next: the PC player's tracks or the songs in the folder
        int rx = bx - 4 - 20;
        if (skips) {
            boolean rh = hover(rx, by + 3, 20, 20);
            g.rect(rx, by + 3, 20, 20, rh ? SURFACE_2 : 0xFF25252B);
            g.rect(rx + 6, by + 9, 2, 8, rh ? TEXT : MUTED);
            for (int i = 0; i < 4; i++) g.rect(rx + 9 + i, by + 13 - i, 1, 1 + i * 2, rh ? TEXT : MUTED);
            hit(rx, by + 3, 20, 20, Playback::previous);
            boolean nh = hover(nx, by + 3, 20, 20);
            g.rect(nx, by + 3, 20, 20, nh ? SURFACE_2 : 0xFF25252B);
            for (int i = 0; i < 4; i++) g.rect(nx + 7 + i, by + 10 + i, 1, 7 - i * 2, nh ? TEXT : MUTED);
            g.rect(nx + 12, by + 9, 2, 8, nh ? TEXT : MUTED);
            hit(nx, by + 3, 20, 20, Playback::next);
        }

        // album art
        var cover = dev.henny.vibeyclient.module.lyrics.render.Cover.texture();
        int tx = x + 10;
        if (cover != null && (song != null || pc)) {
            g.image(cover, x + 8, y + 7, 28, 28, dev.henny.vibeyclient.module.lyrics.render.Cover.SIZE);
            tx = x + 42;
        }

        int textW = (skips ? rx : bx) - tx - 8;
        if (pc) {
            NowPlaying.Track t = NowPlaying.track();
            String head = t == null ? T.get("pc.idle") : t.title();
            g.text(Component.literal(g.clip(head, textW - 8)).withStyle(ChatFormatting.BOLD), tx, y + 9, t == null ? MUTED : TEXT);
            String sub;
            int color = MUTED;
            if (NowPlaying.state() == NowPlaying.State.UNSUPPORTED) {
                sub = T.get("pc.unsupported." + osKey());
                color = AMBER;
            } else if (t == null) {
                sub = T.get(NowPlaying.state() == NowPlaying.State.STARTING ? "pc.starting" : "pc.idle.hint");
                color = DIM;
            } else {
                String app = t.appName().isEmpty() ? "" : t.appName() + " · ";
                switch (PcLyrics.status()) {
                    case LOADING -> sub = app + T.get("pc.loading") + dots();
                    case NOT_FOUND -> {
                        sub = app + T.get("pc.notfound");
                        color = AMBER;
                    }
                    case OFFLINE -> {
                        sub = app + T.get("pc.offline");
                        color = AMBER;
                    }
                    case ERROR -> {
                        sub = app + T.get("pc.error");
                        color = RED;
                    }
                    default -> sub = app + t.artist();
                }
            }
            g.text(g.clip(sub, textW), tx, y + 21, color);
        } else if (song == null) {
            Song next = SongLibrary.find(LyricsConfig.get().song);
            g.text(T.get("nothing"), tx, y + 10, MUTED);
            String sub = next != null ? T.get("resume", next.label()) : T.get("pick");
            g.text(g.clip(sub, textW), tx, y + 22, DIM);
        } else {
            g.text(Component.literal(g.clip(song.title(), textW - 8)).withStyle(ChatFormatting.BOLD), tx, y + 9, TEXT);
            String sub = song.artist().isBlank() ? "" : song.artist();
            if (!song.hasAudio()) sub = T.get("external");
            else if (!song.hasLyrics()) sub = T.get("nolyrics.playing");
            g.text(g.clip(sub, textW), tx, y + 21, song.hasLyrics() ? MUTED : AMBER);
        }

        // Progress
        long pos = Playback.positionMs();
        long dur = Playback.durationMs();
        int py = y + h - 12;
        String time = clock(pos) + " / " + clock(dur);
        int tw = g.width(time);
        int barW = w - 20 - tw - 8;
        bar(g, x + 10, py + 2, barW, 3, dur > 0 ? (float) pos / dur : 0f,
                song == null || dur <= 0 || pc ? null : f -> Playback.seek(Math.round(f * dur)));
        g.text(time, x + w - 10 - tw, py - 1, DIM);

        String err = Playback.error();
        if (err != null && song != null) {
            g.text(g.clip(T.get("error", err), w), x, y + h + 3, RED);
            return y + h + 12;
        }
        // Lyrics without audio: line them up with music that plays somewhere else.
        if ((song != null && !song.hasAudio()) || (pc && PcLyrics.lyrics() != null)) {
            int ny = y + h + 4;
            g.text(g.clip(T.get("nudge"), w - 120), x, ny + 4, DIM);
            int b2 = buttonRight(g, x + w, ny, 16, "+0.5s", Style.SECONDARY, () -> Playback.nudge(500));
            buttonRight(g, b2 - 4, ny, 16, "−0.5s", Style.SECONDARY, () -> Playback.nudge(-500));
            return ny + 16;
        }
        return y + h;
    }

    private int songs(Gfx g, int x, int y, int w) {
        y = section(g, T.get("songs"), x, y);
        {
            int h = 30;
            boolean on = Playback.isPc();
            boolean hov = hover(x, y, w, h);
            g.rect(x, y, w, h, on ? 0xFF2A1F33 : (hov ? SURFACE_HOVER : SURFACE));
            if (on) g.rect(x, y, 2, h, ACCENT);
            g.text(Component.literal("♫ " + T.get("pc")).withStyle(ChatFormatting.BOLD), x + 9, y + 5, TEXT);
            g.text(g.clip(T.get(on ? "pc.on" : "pc.desc"), w - 60), x + 9, y + 17, DIM);
            toggle(g, "pc", x + w - 8, y + h / 2, on, () -> Playback.setPc(!Playback.isPc()));
            hit(x, y, w - 40, h, () -> Playback.setPc(!Playback.isPc()));
            y += h + 2;
        }
        List<Song> list = SongLibrary.songs();
        if (list.isEmpty()) return note(g, x, y, w, T.get("songs.empty"), MUTED);
        Song current = Playback.song();
        for (Song s : list) {
            int h = 22;
            boolean cur = current != null && current.id().equals(s.id());
            boolean hov = hover(x, y, w, h);
            g.rect(x, y, w, h, cur ? 0xFF2A1F33 : (hov ? SURFACE_HOVER : SURFACE));
            if (cur) g.rect(x, y, 2, h, ACCENT);

            int right = x + w - 6;
            // Songs without lyrics get a direct way to find them.
            if (!s.hasLyrics()) {
                right = buttonRight(g, right, y + 3, 16, T.get("find"), Style.PRIMARY,
                        () -> LyricsCompat.openScreen(new SearchScreen(this, s))) - 6;
            } else {
                String badge = s.hasAudio() ? "♪" : T.get("badge.lrc");
                int bw = g.width(badge) + 8;
                g.rect(right - bw, y + 5, bw, 12, SURFACE_2);
                g.textCenter(badge, right - bw / 2, y + 7, s.hasAudio() ? MUTED : AMBER);
                right -= bw + 6;
            }
            int tx = x + 9;
            int avail = right - tx;
            String title = g.clip(s.title(), avail);
            g.text(title, tx, y + 7, cur ? TEXT : TEXT);
            int used = g.width(title) + 6;
            if (!s.artist().isBlank() && used < avail - 20) {
                g.text(g.clip(s.artist(), avail - used), tx + used, y + 7, DIM);
            }
            hit(x, y, right - x, h, () -> Playback.play(s));
            y += h + 2;
        }
        return y - 2;
    }

    @Override
    protected void drawFooter(Gfx g, int x, int bottom, int w) {
        int y = bottom + 8;
        int bx = x + PAD;
        int bw = w - PAD * 2;
        int third = (bw - 8) / 3;
        button(g, bx, y, third, 20, T.get("folder"), Style.SECONDARY,
                () -> LyricsCompat.openFolder(LyricsModule.songsDir()));
        button(g, bx + third + 4, y, third, 20, T.get("find"), Style.SECONDARY,
                () -> LyricsCompat.openScreen(new SearchScreen(this, null)));
        button(g, bx + (third + 4) * 2, y, bw - (third + 4) * 2, 20, T.get("advanced"), Style.SECONDARY,
                () -> LyricsCompat.openScreen(new AdvancedScreen(this)));
    }

    /** Pins the lyrics a few blocks in front of you, a bit above eye level. */
    public static void placeHere() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null) return;
        LyricsConfig cfg = LyricsConfig.get();
        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.lengthSqr() < 1e-4) flat = new Vec3(0, 0, 1);
        Vec3 p = eye.add(flat.normalize().scale(8)).add(0, 2.5, 0);
        cfg.anchorX = p.x;
        cfg.anchorY = p.y;
        cfg.anchorZ = p.z;
        cfg.anchorYaw = mc.player.getYRot();
        cfg.anchorSet = true;
        cfg.save();
    }

    static void aimSky() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null) return;
        LyricsConfig cfg = LyricsConfig.get();
        cfg.skyYaw = mc.player.getYRot();
        cfg.skyMode = LyricsConfig.SkyMode.FIXED;
        cfg.save();
    }

    private static String osKey() {
        String n = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return n.contains("win") ? "windows" : n.contains("mac") ? "mac" : "linux";
    }

    static String clock(long ms) {
        long s = Math.max(0, ms) / 1000;
        return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }
}
