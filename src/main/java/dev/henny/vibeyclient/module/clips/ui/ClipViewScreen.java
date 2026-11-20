package dev.henny.vibeyclient.module.clips.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.Tasks;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.clips.ClipSaver;
import dev.henny.vibeyclient.module.clips.ClipsFiles;
import dev.henny.vibeyclient.module.clips.store.ClipStore;
import dev.henny.vibeyclient.media.ffmpeg.Ffmpeg;
import dev.henny.vibeyclient.media.player.FfmpegPlayer;
import dev.henny.vibeyclient.media.player.MediaPlayer;
import dev.henny.vibeyclient.ui.CardScreen;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Icons;
import dev.henny.vibeyclient.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.util.Locale;

/**
 * One clip: plays it with the video screens' FFmpeg player (decoded in its own process, drawn into the card), and
 * Keep, Rename, Delete (asks once more), Open folder, Copy path. Edits go through {@link ClipStore} (unknown fields
 * stay, JSON written atomically), so the launcher sees them at once.
 */
public final class ClipViewScreen extends CardScreen {
    private static final int PLAY_W = 960, PLAY_H = 540;

    private final ClipsGalleryScreen gallery;
    private volatile ClipStore.Clip clip;
    private MediaPlayer player;
    private String playerError;
    private EditBox nameBox;
    private boolean renaming;
    private long deleteArmedUntil;
    private String flash;
    private long flashUntil;

    public ClipViewScreen(ClipsGalleryScreen gallery, ClipStore.Clip clip) {
        super(Component.literal(Lang.tr("clips.gallery.title")), gallery, "clips.view.");
        this.gallery = gallery;
        this.clip = clip;
    }

    @Override
    protected void init() {
        super.init();
        if (nameBox == null) {
            nameBox = new EditBox(Minecraft.getInstance().font, 0, 0, 200, 12, Component.empty());
            nameBox.setMaxLength(ClipStore.MAX_TITLE);
        }
        input(nameBox, this::saveName); // again after a resize cleared the widgets
        if (player == null && playerError == null) startPlayer(0, true);
    }

    private void startPlayer(long atMs, boolean playing) {
        String ff = Ffmpeg.find();
        if (ff == null) {
            playerError = Lang.tr("clips.view.noFfmpeg");
            return;
        }
        int w = Math.min(PLAY_W, Math.max(2, clip.integer("width", PLAY_W)));
        int h = Math.min(PLAY_H, Math.max(2, clip.integer("height", PLAY_H)));
        int fps = Math.max(1, Math.min(60, clip.integer("fps", 30)));
        player = new FfmpegPlayer(ff, clip.video().toAbsolutePath().toString(), w, h, fps, atMs, playing);
        player.audio(1f, null);
    }

    /** The player (tests). */
    public MediaPlayer player() {
        return player;
    }

    public ClipStore.Clip clip() {
        return clip;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    protected int cardWidth() {
        return 560;
    }

    @Override
    protected int fixedHeight() {
        return height - 30;
    }

    @Override
    protected int headerHeight() {
        return 34;
    }

    @Override
    protected int footerHeight() {
        return 40;
    }

    private String name() {
        ClipStore.Clip c = clip;
        return c.title() != null ? c.title() : ClipsGalleryScreen.where(c);
    }

    @Override
    protected void drawHeader(Gfx g, int x, int y, int w) {
        // back arrow
        boolean hov = hover(x + PAD - 3, y + 9, 14, 15);
        if (hov) g.rect(x + PAD - 3, y + 9, 14, 15, SURFACE_HOVER);
        Icons.draw(g, Icons.BACK, x + PAD, y + 12, hov ? TEXT : MUTED);
        hit(x + PAD - 3, y + 9, 14, 15, this::onClose);
        int tx = x + PAD + 16;
        int closeX = closeButton(g, x, y, w);
        title(g, g.clip(name(), closeX - tx - 30), tx, y + 13);
        if (clip.kept()) Icons.draw(g, Icons.STAR, tx + Math.min(g.boldWidth(name()), closeX - tx - 30) + 6, y + 12, Theme.WARN);
    }

    @Override
    protected int drawBody(Gfx g, int x, int y, int w) {
        ClipStore.Clip c = clip;
        int vw = w;
        int vh = vw * 9 / 16;
        int maxH = Math.max(90, height - 30 - 34 - 40 - 110);
        if (vh > maxH) {
            vh = maxH;
            vw = vh * 16 / 9;
        }
        int vx = x + (w - vw) / 2;
        g.rect(vx, y, vw, vh, 0xFF000000);
        MediaPlayer p = player;
        if (p != null) {
            p.frame(true);
            Identifier tex = p.texture();
            if (tex != null) g.image(tex, vx, y, vw, vh, 0, 0, p.width(), p.height(), p.width(), p.height());
            if (p.status() == MediaPlayer.Status.ERROR) g.textCenter(g.clip(String.valueOf(p.error()), vw - 10), vx + vw / 2, y + vh / 2 - 4, RED);
            else if (tex == null) g.textCenter(Lang.tr("clips.view.loading") + dots(), vx + vw / 2, y + vh / 2 - 4, MUTED);
            if (p.status() == MediaPlayer.Status.PAUSED || p.status() == MediaPlayer.Status.ENDED) {
                g.rect(vx + vw / 2 - 14, y + vh / 2 - 14, 28, 28, 0x90000000);
                playGlyph(g, vx + vw / 2 - 4, y + vh / 2 - 7, 0xFFFFFFFF);
            }
            hit(vx, y, vw, vh, this::togglePlay);
        } else if (playerError != null) {
            g.textCenter(g.clip(playerError, vw - 10), vx + vw / 2, y + vh / 2 - 4, MUTED);
        }
        y += vh + 6;

        // controls: play/pause, seek bar, time
        if (p != null) {
            boolean playing = p.status() == MediaPlayer.Status.PLAYING;
            boolean hb = hover(vx, y, 18, 14);
            g.rect(vx, y, 18, 14, hb ? SURFACE_HOVER : SURFACE);
            if (playing) {
                g.rect(vx + 6, y + 3, 2, 8, TEXT);
                g.rect(vx + 10, y + 3, 2, 8, TEXT);
            } else {
                playGlyph(g, vx + 6, y + 3, TEXT);
            }
            hit(vx, y, 18, 14, this::togglePlay);
            long dur = p.durationMs() > 0 ? p.durationMs() : c.durationMs();
            long pos = Math.min(dur, p.positionMs());
            String time = ClipSaver.duration(pos) + " / " + ClipSaver.duration(dur);
            int tw = g.width(time);
            int bx = vx + 24, bw = vw - 24 - tw - 8;
            bar(g, bx, y + 5, bw, 4, dur <= 0 ? 0 : (float) pos / dur, f -> seek((long) (f * dur)));
            // event marks (kills) on the bar
            if (c.json().get("events") instanceof JsonArray events && dur > 0) {
                for (JsonElement e : events) {
                    if (!(e instanceof JsonObject o) || !o.has("t_ms")) continue;
                    int mx = bx + (int) (bw * Math.min(1.0, o.get("t_ms").getAsLong() / (double) dur));
                    g.rect(mx - 1, y + 2, 2, 10, 0xFFFF6B4A);
                }
            }
            g.text(time, vx + vw - tw, y + 3, MUTED);
            y += 20;
        }

        // rename
        if (renaming) {
            g.text(Lang.tr("clips.view.rename.label"), x, y + 4, MUTED);
            int bx = x + 70;
            inputBox(g, nameBox, bx, y, w - 70 - 130);
            int right = x + w;
            int l = buttonRight(g, right, y - 1, 18, Lang.tr("clips.view.rename.save"), Style.PRIMARY, this::saveName);
            buttonRight(g, l - 6, y - 1, 18, Lang.tr("clips.view.rename.cancel"), Style.SECONDARY, () -> renaming = false);
            y += 24;
        }

        // facts
        String reason = Lang.tr("clips.reason." + ClipSaver.reasonKey(c.reason()));
        y = fact(g, x, y, w, Lang.tr("clips.view.reason"), reason + (c.integer("kills", 0) > 1 ? " · " + Lang.tr("clips.view.kills", c.integer("kills", 0)) : ""));
        String server = c.str("server"), world = c.str("world");
        if (server != null) y = fact(g, x, y, w, Lang.tr("clips.view.server"), server);
        else if (world != null) y = fact(g, x, y, w, Lang.tr("clips.view.world"), world);
        y = fact(g, x, y, w, Lang.tr("clips.view.date"), ClipsGalleryScreen.DATE.format(c.createdAt()));
        y = fact(g, x, y, w, Lang.tr("clips.view.length"), ClipSaver.duration(c.durationMs()) + "  ·  " + c.integer("width", 0) + "×" + c.integer("height", 0)
                + " @ " + c.integer("fps", 0) + " fps  ·  " + String.format(Locale.ROOT, "%.1f MB", c.sizeBytes() / 1048576.0));
        if (c.json().get("events") instanceof JsonArray events && !events.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (JsonElement e : events) {
                if (!(e instanceof JsonObject o)) continue;
                if (!sb.isEmpty()) sb.append(", ");
                String type = o.has("type") ? o.get("type").getAsString() : "";
                String victim = o.has("victim") ? o.get("victim").getAsString() : "";
                long t = o.has("t_ms") ? o.get("t_ms").getAsLong() : 0;
                sb.append(ClipSaver.duration(t)).append(' ').append(type.equals("death") ? Lang.tr("clips.view.event.death") : victim);
            }
            y = fact(g, x, y, w, Lang.tr("clips.view.events"), sb.toString());
        }
        if (c.str("encoder") != null) y = fact(g, x, y, w, Lang.tr("clips.view.encoder"), c.str("encoder"));
        if (flash != null && System.currentTimeMillis() < flashUntil) {
            g.text(flash, x, y + 4, GREEN);
            y += 14;
        }
        return y;
    }

    private static void playGlyph(Gfx g, int x, int y, int color) {
        for (int i = 0; i < 4; i++) g.rect(x + i, y + i, 1, 8 - i * 2, color);
        for (int i = 0; i < 4; i++) g.rect(x + i, y + i + 1, 1, Math.max(1, 6 - i * 2), color);
    }

    private int fact(Gfx g, int x, int y, int w, String label, String value) {
        g.text(label, x, y, DIM);
        g.text(g.clip(value, w - 80), x + 80, y, TEXT);
        return y + 12;
    }

    @Override
    protected void drawFooter(Gfx g, int x, int y, int w) {
        int by = y + 11;
        int lx = x + PAD;
        ClipStore.Clip c = clip;
        String keep = (c.kept() ? Lang.tr("clips.view.kept") : Lang.tr("clips.view.keep"));
        int kw = g.width(keep) + 34; // room for the star left of the centred label
        button(g, lx, by, kw, 18, keep, c.kept() ? Style.PRIMARY : Style.SECONDARY, this::toggleKeep);
        Icons.draw(g, Icons.STAR, lx + 6, by + 5, c.kept() ? 0xFFFFFFFF : Theme.WARN);
        lx += kw + 6;
        lx += button(g, lx, by, g.width(Lang.tr("clips.view.rename")) + 14, 18, Lang.tr("clips.view.rename"), Style.SECONDARY, this::startRename) + 6;
        boolean armed = System.currentTimeMillis() < deleteArmedUntil;
        String del = (armed ? Lang.tr("clips.view.deleteConfirm") : Lang.tr("clips.view.delete"));
        button(g, lx, by, g.width(del) + 14, 18, del, Style.DANGER, this::delete);
        int right = x + w - PAD;
        int l = buttonRight(g, right, by, 18, Lang.tr("clips.view.copyPath"), Style.SECONDARY, () -> {
            ClientCompat.setClipboard(clip.video().toAbsolutePath().toString());
            flash(Lang.tr("clips.view.copied"));
        });
        buttonRight(g, l - 6, by, 18, Lang.tr("clips.view.folder"), Style.SECONDARY, ClipsFiles::openFolder);
    }

    private void flash(String s) {
        flash = s;
        flashUntil = System.currentTimeMillis() + 2500;
    }

    // ============================================================================================ actions

    public void togglePlay() {
        MediaPlayer p = player;
        if (p == null) return;
        MediaPlayer.Status s = p.status();
        if (s == MediaPlayer.Status.ENDED) {
            p.seek(0);
            p.play();
        } else if (s == MediaPlayer.Status.PLAYING) {
            p.pause();
        } else {
            p.play();
        }
    }

    public void seek(long ms) {
        MediaPlayer p = player;
        if (p != null) p.seek(Math.max(0, ms));
    }

    public void toggleKeep() {
        boolean to = !clip.kept();
        edit(to, null, to ? Lang.tr("clips.view.keptFlash") : Lang.tr("clips.view.unkeptFlash"));
    }

    public void startRename() {
        renaming = true;
        nameBox.setValue(clip.title() == null ? "" : clip.title());
        nameBox.setFocused(true);
        setFocused(nameBox);
    }

    public void rename(String title) {
        edit(null, title == null ? "" : title, Lang.tr("clips.view.renamedFlash"));
        renaming = false;
    }

    private void saveName() {
        rename(nameBox.getValue());
    }

    private void edit(Boolean kept, String title, String done) {
        String id = clip.id();
        ClipsFiles.io(() -> {
            try {
                ClipStore.Clip c = ClipsFiles.store().edit(id, kept, title, null);
                Tasks.onMain(() -> {
                    if (c != null) clip = c;
                    flash(done);
                    gallery.rescan();
                });
            } catch (IOException e) {
                Vibey.LOG.warn("Clips: edit failed", e);
                Tasks.onMain(() -> flash(e.getMessage()));
            }
        });
    }

    public void delete() {
        long now = System.currentTimeMillis();
        if (now > deleteArmedUntil) {
            deleteArmedUntil = now + 3000;
            return;
        }
        String id = clip.id();
        if (player != null) {
            player.close(); // release the file before deleting it
            player = null;
        }
        playerError = Lang.tr("clips.view.deleted");
        ClipsFiles.io(() -> {
            ClipStore store = ClipsFiles.store();
            store.delete(id);
            // the player's FFmpeg may hold the video open for a moment after closing (Windows file lock)
            for (int i = 0; i < 20 && !store.retryPending(); i++) {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    break;
                }
            }
            Tasks.onMain(() -> {
                gallery.forget(id);
                gallery.rescan();
                ClientCompat.openScreen(gallery);
            });
        });
    }

    @Override
    protected boolean key(int keyCode) {
        if (!renaming && keyCode == 32) { // space
            togglePlay();
            return true;
        }
        return super.key(keyCode);
    }

    @Override
    public void removed() {
        if (player != null) {
            player.close();
            player = null;
        }
        super.removed();
    }

    @Override
    public void onClose() {
        if (player != null) {
            player.close();
            player = null;
        }
        gallery.rescan();
        ClientCompat.openScreen(gallery);
    }
}
