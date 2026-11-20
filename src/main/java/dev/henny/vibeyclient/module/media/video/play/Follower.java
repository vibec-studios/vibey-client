package dev.henny.vibeyclient.module.media.video.play;

import dev.henny.vibeyclient.media.player.MediaPlayer;
import dev.henny.vibeyclient.module.media.video.MediaScreen;
import dev.henny.vibeyclient.module.media.video.MediaSource;
import dev.henny.vibeyclient.module.media.video.Video;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Follower {
    private static final long OWN_DRIFT_MS = 750;
    private static final long CORRECTION_GAP_MS = 5000;

    private final Video video;
    private final Map<UUID, Boolean> appliedPlaying = new HashMap<>();
    private final Map<UUID, Long> lastCorrection = new HashMap<>();

    Follower(Video video) {
        this.video = video;
    }

    public void forget(UUID id) {
        appliedPlaying.remove(id);
    }

    void applied(UUID id, boolean playing) {
        appliedPlaying.put(id, playing);
    }

    public void corrected(UUID id) {
        lastCorrection.put(id, System.currentTimeMillis());
    }

    void follow(MediaScreen s, int range) {
        MediaPlayer p = s.player;
        if (p.durationMs() > 0) s.timeline.durationMs(p.durationMs());
        applyPlaying(s, p);
        MediaPlayer.Status st = p.status();
        if (!s.remote && !s.started && st == MediaPlayer.Status.PLAYING) startOwn(s, p);
        if (st == MediaPlayer.Status.ENDED && s.playing()) ended(s, p);
        else if (!s.remote) keepOwn(s, p, st);
        else followRemote(s, p, st);
        volume(s, p, range);
    }

    private void applyPlaying(MediaScreen s, MediaPlayer p) {
        Boolean applied = appliedPlaying.get(s.id);
        if (applied != null && applied == s.playing()) return;
        if (s.playing()) p.play();
        else p.pause();
        appliedPlaying.put(s.id, s.playing());
        boolean seekable = p.seekable() || s.timeline.durationMs() > 0;
        if (applied == null && seekable && Math.abs(p.positionMs() - s.expectedMs()) > 1500) p.seek(s.expectedMs());
    }

    private void startOwn(MediaScreen s, MediaPlayer p) {
        s.started = true;
        if (Math.abs(p.positionMs() - s.expectedMs()) < 5000) {
            s.setTimeline(true, p.positionMs());
            video.net.sendState(s);
        }
    }

    private void ended(MediaScreen s, MediaPlayer p) {
        if (s.loop) {
            if (!s.remote) {
                s.setTimeline(true, 0);
                video.net.sendState(s);
            }
            p.seek(0);
            p.play();
        } else if (!s.remote) {
            long d = s.timeline.durationMs();
            s.setTimeline(false, d > 0 ? d : p.positionMs());
            appliedPlaying.put(s.id, false);
            video.net.sendState(s);
        }
    }

    private void keepOwn(MediaScreen s, MediaPlayer p, MediaPlayer.Status st) {
        boolean seekable = p.seekable() || s.timeline.durationMs() > 0;
        if (st != MediaPlayer.Status.PLAYING || !s.playing() || !s.started || !seekable) return;
        long now = System.currentTimeMillis();
        long behind = s.expectedMs() - p.positionMs();
        if (Math.abs(behind) > OWN_DRIFT_MS && now - lastCorrection.getOrDefault(s.id, 0L) > CORRECTION_GAP_MS) {
            lastCorrection.put(s.id, now);
            s.setTimeline(true, p.positionMs());
            video.net.sendState(s);
        }
    }

    private void followRemote(MediaScreen s, MediaPlayer p, MediaPlayer.Status st) {
        boolean seekable = p.seekable() || s.timeline.durationMs() > 0;
        if (st != MediaPlayer.Status.PLAYING && st != MediaPlayer.Status.PAUSED || !seekable) return;
        long behind = s.expectedMs() - p.positionMs();
        if (s.correcting && st == MediaPlayer.Status.PLAYING) {
            s.correcting = false;
            s.lagMs = Math.max(0, Math.min(5000, s.lagMs + behind));
        }
        long now = System.currentTimeMillis();
        long limit = s.playing() ? (s.source.kind() == MediaSource.Kind.FILE ? 2000 : 3000) : 600;
        if (Math.abs(behind) > limit && now - lastCorrection.getOrDefault(s.id, 0L) > CORRECTION_GAP_MS) {
            lastCorrection.put(s.id, now);
            s.correcting = s.playing();
            p.seek(s.expectedMs() + (s.playing() ? s.lagMs : 0));
        }
    }

    private void volume(MediaScreen s, MediaPlayer p, int range) {
        float gain = video.cfg.volume.get() / 100f * s.volume * soundVolume();
        if (s.id.equals(video.overlayId)) {
            p.audio(gain, null);
            return;
        }
        float t = (float) Math.max(0, Math.min(1, 1 - (s.distance - 3) / Math.max(1, range - 3)));
        p.audio(gain * t * t, s.center());
    }

    private static float soundVolume() {
        var o = Minecraft.getInstance().options;
        return o.getSoundSourceVolume(SoundSource.MASTER) * o.getSoundSourceVolume(SoundSource.RECORDS);
    }
}
