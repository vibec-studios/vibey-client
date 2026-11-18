package dev.henny.vibeyclient.module.media.lyrics;

import dev.henny.vibeyclient.module.media.lyrics.audio.AudioPlayer;
import dev.henny.vibeyclient.module.media.lyrics.audio.Decoder;
import dev.henny.vibeyclient.module.media.lyrics.library.Song;
import dev.henny.vibeyclient.module.media.lyrics.library.SongLibrary;
import dev.henny.vibeyclient.module.media.lyrics.lyrics.Lyrics;
import dev.henny.vibeyclient.module.media.lyrics.pc.NowPlaying;
import dev.henny.vibeyclient.module.media.lyrics.pc.PcLyrics;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;

/**
 * What is playing right now. With an audio file the sound card is the clock; with lyrics only (music plays
 * elsewhere) a timer is, and {@link #nudge} lets you line it up by ear.
 */
public final class Playback {
    private static Song song;
    private static Lyrics lyrics;
    private static AudioPlayer audio;
    private static boolean playing;
    private static long timerBase;
    private static long timerStart;
    private static String error;
    /** Lyrics follow the music that plays on the PC instead of a song from the folder. */
    private static boolean pc;
    /** Lyrics hidden with the hide key; the music keeps playing. */
    private static boolean hidden;

    private Playback() {}

    public static boolean isPc() {
        return pc;
    }

    /** Switches "music from the PC" on or off (and remembers it). */
    public static void setPc(boolean on) {
        LyricsConfig cfg = LyricsConfig.get();
        if (on == pc) return;
        if (on) {
            stopLocal();
            NowPlaying.start();
            PcLyrics.reset();
        } else {
            NowPlaying.stop();
            PcLyrics.reset();
        }
        pc = on;
        cfg.pcMode = on;
        if (on && cfg.skyMode == LyricsConfig.SkyMode.FIXED && Minecraft.getInstance().player != null) cfg.skyYaw = currentYaw();
        cfg.save();
    }

    /** Game closes: stop everything without touching the saved settings. */
    public static void shutdown() {
        NowPlaying.stop();
        stopLocal();
    }

    /** Module switched off: stop the music and the PC follower, keep the saved settings. */
    public static void suspend() {
        NowPlaying.stop();
        PcLyrics.reset();
        pc = false;
        stopLocal();
    }

    public static Song song() {
        return pc ? PcLyrics.song() : song;
    }

    public static Lyrics lyrics() {
        return pc ? PcLyrics.lyrics() : lyrics;
    }

    public static boolean isPlaying() {
        return pc ? NowPlaying.isPlaying() : playing;
    }

    /** The player of the song from the folder that plays now, or null (PC music, lyrics only, nothing). */
    public static AudioPlayer audio() {
        return pc ? null : audio;
    }

    public static boolean hasAudio() {
        return !pc && audio != null;
    }

    public static String error() {
        if (pc) return PcLyrics.status() == PcLyrics.Status.ERROR ? PcLyrics.error() : null;
        if (audio != null && audio.error() != null) return audio.error();
        return error;
    }

    /** Starts a song from the beginning. */
    public static void play(Song s) {
        if (pc) setPc(false);
        stop();
        song = s;
        error = null;
        try {
            lyrics = SongLibrary.loadLyrics(s);
        } catch (Exception e) {
            lyrics = null;
            error = e.getMessage();
            LyricsFeature.LOG.warn("Could not read lyrics {}: {}", s.lrc(), e.toString());
        }
        if (s.hasAudio()) {
            try {
                audio = new AudioPlayer(Decoder.open(s.audio()));
                audio.setVolume(volume());
            } catch (Exception e) {
                audio = null;
                error = e.getMessage();
                LyricsFeature.LOG.warn("Could not play {}: {}", s.audio(), e.toString());
            }
        }
        timerBase = 0;
        timerStart = System.nanoTime();
        playing = true;
        LyricsConfig cfg = LyricsConfig.get();
        cfg.song = s.id();
        // A fixed sky direction faces where you look when the song starts.
        if (cfg.skyMode == LyricsConfig.SkyMode.FIXED) cfg.skyYaw = currentYaw();
        cfg.save();
    }

    public static void toggle() {
        if (pc) {
            NowPlaying.togglePlayPause();
            return;
        }
        if (song == null) {
            Song s = SongLibrary.find(LyricsConfig.get().song);
            if (s == null && !SongLibrary.songs().isEmpty()) s = SongLibrary.songs().get(0);
            if (s != null) play(s);
            return;
        }
        if (playing) pause();
        else resume();
    }

    /** Next song: the PC player's next track, or the next song in the folder. */
    public static void next() {
        if (pc) {
            NowPlaying.next();
            return;
        }
        step(1);
    }

    /** Back to the start of the song, or (in its first seconds) to the one before. */
    public static void previous() {
        if (pc) {
            NowPlaying.previous();
            return;
        }
        if (song != null && positionMs() > 3000) {
            restart();
            return;
        }
        step(-1);
    }

    private static void step(int dir) {
        java.util.List<Song> songs = SongLibrary.songs();
        if (songs.isEmpty()) return;
        String cur = song != null ? song.id() : LyricsConfig.get().song;
        int idx = -1;
        for (int i = 0; i < songs.size(); i++) if (songs.get(i).id().equals(cur)) idx = i;
        int n = idx < 0 ? 0 : Math.floorMod(idx + dir, songs.size());
        play(songs.get(n));
    }

    public static boolean hidden() {
        return hidden;
    }

    public static void toggleHidden() {
        hidden = !hidden;
    }

    public static void pause() {
        if (!playing) return;
        timerBase = positionMs();
        playing = false;
        if (audio != null) audio.setPaused(true);
    }

    public static void resume() {
        if (playing || song == null) return;
        if (audio != null && audio.isEnded()) {
            seek(0);
        }
        timerStart = System.nanoTime();
        playing = true;
        if (audio != null) audio.setPaused(false);
    }

    public static void restart() {
        if (song == null) return;
        seek(0);
        resume();
    }

    public static void stop() {
        if (pc) setPc(false);
        stopLocal();
    }

    private static void stopLocal() {
        if (audio != null) audio.stop();
        audio = null;
        song = null;
        lyrics = null;
        playing = false;
        timerBase = 0;
    }

    public static void seek(long ms) {
        if (pc) return;
        ms = Math.max(0, Math.min(ms, durationMs()));
        timerBase = ms;
        timerStart = System.nanoTime();
        if (audio != null) audio.seekMs(ms);
    }

    /** Shifts a timer-run song (no audio file) by {@code ms}. */
    public static void nudge(long ms) {
        if (pc) {
            // The PC player owns the position: shift the lyrics instead.
            LyricsConfig.get().offsetMs += (int) ms;
            LyricsConfig.get().save();
            return;
        }
        seek(positionMs() + ms);
    }

    public static long positionMs() {
        if (pc) return NowPlaying.positionMs();
        if (song == null) return 0;
        if (audio != null) return audio.positionMs();
        if (!playing) return timerBase;
        return timerBase + (System.nanoTime() - timerStart) / 1_000_000L;
    }

    /** The time used to pick the line: song position plus the sync offset. */
    public static long lyricTimeMs() {
        return positionMs() + LyricsConfig.get().offsetMs;
    }

    public static long durationMs() {
        if (pc) {
            NowPlaying.Track t = NowPlaying.track();
            if (t != null && t.durationMs() > 0) return t.durationMs();
            Lyrics l = PcLyrics.lyrics();
            return l == null ? 0 : l.lastTime();
        }
        if (audio != null) {
            long l = audio.lengthMs();
            if (l > 0) return l;
        }
        if (lyrics != null) return lyrics.lastTime();
        return 0;
    }

    /** Once per client tick. */
    public static void tick(Minecraft mc) {
        dev.henny.vibeyclient.module.media.lyrics.render.Cover.tick();
        if (pc) {
            PcLyrics.tick();
            return;
        }
        if (song == null) return;
        if (audio != null) {
            audio.setVolume(volume());
            if (playing && audio.isEnded()) {
                playing = false;
                timerBase = positionMs();
            }
        } else if (playing && positionMs() >= durationMs() && durationMs() > 0) {
            playing = false;
            timerBase = durationMs();
        }
        if (playing && audio != null && LyricsConfig.get().muteVanillaMusic) {
            mc.getMusicManager().stopPlaying();
        }
    }

    private static float volume() {
        Minecraft mc = Minecraft.getInstance();
        float master = mc.options == null ? 1f : mc.options.getSoundSourceVolume(SoundSource.MASTER);
        return LyricsConfig.get().volume / 100f * master;
    }

    private static float currentYaw() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? 0f : mc.player.getYRot();
    }
}
