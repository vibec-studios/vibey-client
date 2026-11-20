package dev.henny.vibeyclient.module.media.lyrics.world;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.module.media.lyrics.Playback;
import dev.henny.vibeyclient.module.media.lyrics.library.Song;
import dev.henny.vibeyclient.module.media.lyrics.render.Cover;
import dev.henny.vui.bind.State;

import java.util.Locale;

public final class MusicControls {
    public final State<String> title = new State<>("");
    public final State<String> artist = new State<>("");
    public final State<String> time = new State<>("");
    public final State<Float> progress = new State<>(0f);
    public final State<Boolean> playing = new State<>(false);
    public final State<Boolean> hasCover = new State<>(false);
    private int presses;

    public String t(String key) {
        return Lang.tr("media.lyrics.panel." + key);
    }

    public void toggle() {
        presses++;
        Playback.toggle();
    }

    public void next() {
        presses++;
        Playback.next();
    }

    public void previous() {
        presses++;
        Playback.previous();
    }

    public void close() {
        MusicPanel.close();
    }

    public int presses() {
        return presses;
    }

    void refresh() {
        Song song = Playback.song();
        title.set(song == null ? t("nothing") : song.title());
        artist.set(song == null ? "" : song.artist());
        long pos = Playback.positionMs();
        long dur = Playback.durationMs();
        progress.set(dur > 0 ? Math.max(0f, Math.min(1f, pos / (float) dur)) : 0f);
        time.set(song == null ? "" : dur > 0 ? clock(pos) + " / " + clock(dur) : clock(pos));
        playing.set(song != null && Playback.isPlaying());
        hasCover.set(Cover.texture() != null);
    }

    private static String clock(long ms) {
        long t = Math.max(0, ms / 1000);
        return String.format(Locale.ROOT, "%d:%02d", t / 60, t % 60);
    }
}
