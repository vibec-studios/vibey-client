package dev.henny.vibeyclient.module.media.video.ui;

import dev.henny.vibeyclient.module.media.video.MediaScreen;
import dev.henny.vibeyclient.module.media.video.Video;
import dev.henny.vibeyclient.media.player.MediaPlayer;
import dev.henny.vibeyclient.module.media.video.web.Web;
import dev.henny.vibeyclient.ui.CardScreen;
import dev.henny.vibeyclient.ui.Gfx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Locale;

/** Controls of one screen: play/pause, seek, volume, size, light, overlay, move, source, remove. */
public final class ControlsScreen extends CardScreen {
    private final MediaScreen s;

    public ControlsScreen(Screen parent, MediaScreen screen) {
        super(Component.literal(Video.tr("controls.title")), parent, "media.video.");
        this.s = screen;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected int dimColor() {
        return 0x50000000;
    }

    @Override
    protected int cardWidth() {
        return 320;
    }

    @Override
    protected int headerHeight() {
        return 42;
    }

    @Override
    protected int footerHeight() {
        return 40;
    }

    @Override
    protected void drawHeader(Gfx g, int x, int y, int w) {
        title(g, g.clip(s.source == null ? tr("controls.title") : s.source.label(), w - 50), x + PAD, y + 11);
        String sub = s.remote ? tr("controls.by", s.ownerName) : s.source != null && s.source.shareable() && s.shared ? tr("controls.shared") : tr("controls.private");
        g.text(g.clip(sub, w - 50), x + PAD, y + 25, DIM);
        closeButton(g, x, y, w);
    }

    @Override
    protected int drawBody(Gfx g, int x, int y, int w) {
        Video v = Video.get();
        if (!v.screens.containsKey(s.id) && !v.placement.active()) {
            Minecraft.getInstance().execute(this::onClose);
            return y;
        }
        int r = x + w;
        MediaPlayer p = s.player;

        // Preview (the picture as it is, nothing drawn on it).
        Identifier tex = p != null && !p.audioOnly() ? p.texture() : null;
        int ph = Math.min(Math.max(48, height / 4), Math.round(w / Math.max(0.3f, s.aspect())));
        int pw = Math.round(ph * s.aspect());
        int px = x + (w - pw) / 2;
        g.rect(px - 1, y - 1, pw + 2, ph + 2, 0xFF0E0E12);
        if (tex != null) {
            g.image(tex, px, y, pw, ph, 0, 0, p.width(), p.height(), p.width(), p.height());
        } else {
            String line = v.statusLine(s);
            g.textCenter(g.clip(line == null ? tr("status.loading") : line, pw - 10), px + pw / 2, y + ph / 2 - 4, MUTED);
        }
        y += ph + 8;

        SetupNeed need = v.missing(s);
        if (need != null && !s.remote) {
            button(g, x, y, w, 18, tr("controls.setup"), Style.PRIMARY, () -> minecraft.setScreen(SetupScreens.of(this, need)));
            y += 26;
        }

        // Transport
        boolean playing = s.playing;
        button(g, x, y, 64, 18, tr(playing ? "controls.pause" : "controls.play"), Style.PRIMARY, () -> v.setPlaying(s, !s.playing));
        long pos = p != null && p.status() != MediaPlayer.Status.LOADING ? p.positionMs() : s.expectedMs();
        long dur = s.durationMs;
        String time = dur > 0 ? fmt(pos) + " / " + fmt(dur) : fmt(pos);
        g.textRight(time, r, y + 5, MUTED);
        y += 24;
        if (dur > 0) {
            bar(g, x, y, w, 5, pos / (float) dur, f -> v.seek(s, (long) (f * dur)));
            y += 14;
        } else {
            int bw = (w - 6) / 2;
            button(g, x, y, bw, 16, tr("controls.back10"), Style.SECONDARY, () -> v.seek(s, Math.max(0, s.expectedMs() - 10_000)));
            button(g, x + bw + 6, y, bw, 16, tr("controls.fwd10"), Style.SECONDARY, () -> v.seek(s, s.expectedMs() + 10_000));
            y += 24;
        }

        int h = row(g, x, y, w, "controls.volume");
        stepperLabel(g, r, y + h / 2, Math.round(s.volume * 100) + " %",
                () -> setVolume(s.volume - 0.1f), () -> setVolume(s.volume + 0.1f));
        y += h + 2;

        h = row(g, x, y, w, "controls.overlay");
        toggle(g, "overlay", r, y + h / 2, s.id.equals(v.overlayId), () -> v.toggleOverlay(s));
        y += h + 2;

        if (!s.remote) {
            if (s.placed()) {
                h = row(g, x, y, w, "controls.size");
                stepperLabel(g, r, y + h / 2, String.format(Locale.ROOT, "%.2f m", s.width),
                        () -> v.resize(s, s.width - 0.25f), () -> v.resize(s, s.width + 0.25f));
                y += h + 2;
                h = row(g, x, y, w, "controls.glow");
                toggle(g, "glow", r, y + h / 2, s.glowing, () -> {
                    s.glowing = !s.glowing;
                    v.changed(s);
                });
                y += h + 2;
            }
            h = row(g, x, y, w, "controls.loop");
            toggle(g, "loop", r, y + h / 2, s.loop, () -> {
                s.loop = !s.loop;
                v.changed(s);
            });
            y += h + 2;
            if (s.placed() && s.source != null && s.source.shareable()) {
                h = row(g, x, y, w, "controls.share");
                toggle(g, "share", r, y + h / 2, s.shared, () -> {
                    s.shared = !s.shared;
                    v.changed(s);
                });
                y += h + 2;
            }
        }
        return y;
    }

    private void setVolume(float vol) {
        s.volume = Math.max(0f, Math.min(1f, Math.round(vol * 10) / 10f));
        Video.get().changed(s);
    }

    @Override
    protected void drawFooter(Gfx g, int x, int y, int w) {
        Video v = Video.get();
        int by = y + 11;
        int right = x + w - PAD;
        if (s.remote) {
            right = buttonRight(g, right, by, 18, tr("controls.unload"), Style.DANGER, () -> {
                s.accepted = false;
                v.closePlayer(s);
                onClose();
            }) - 6;
        } else {
            right = buttonRight(g, right, by, 18, tr("controls.remove"), Style.DANGER, () -> {
                v.remove(s);
                onClose();
            }) - 6;
            right = buttonRight(g, right, by, 18, tr("controls.source"), Style.SECONDARY, () -> minecraft.setScreen(new SourceScreen(this, s))) - 6;
            if (s.placed()) right = buttonRight(g, right, by, 18, tr("controls.move"), Style.SECONDARY, () -> v.placement.start(s, true)) - 6;
        }
        if (s.player != null && s.player.interactive() && Web.installed()) {
            buttonRight(g, right, by, 18, tr("controls.interact"), Style.SECONDARY, () -> Web.openBrowser(this, s.player));
        }
    }

    public static String fmt(long ms) {
        long t = Math.max(0, ms / 1000);
        long hh = t / 3600, mm = t / 60 % 60, ss = t % 60;
        return hh > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", hh, mm, ss) : String.format(Locale.ROOT, "%d:%02d", mm, ss);
    }
}
