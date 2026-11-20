package dev.henny.vibeyclient.module.media.video.controls;

import dev.henny.vibeyclient.media.player.MediaPlayer;
import dev.henny.vibeyclient.module.media.video.MediaScreen;
import dev.henny.vibeyclient.module.media.video.Video;

import java.util.Locale;

final class ScreenState {
    private final ScreenControls c;
    private final MediaScreen s;

    ScreenState(ScreenControls controls, MediaScreen screen) {
        this.c = controls;
        this.s = screen;
    }

    void refresh(Video video) {
        MediaPlayer p = s.player;
        long pos = p != null && p.status() != MediaPlayer.Status.LOADING ? p.positionMs() : s.expectedMs();
        long dur = s.timeline.durationMs();
        c.playing.set(s.playing());
        c.seekable.set(dur > 0);
        c.progress.set(dur > 0 ? Math.max(0f, Math.min(1f, pos / (float) dur)) : 0f);
        c.time.set(dur > 0 ? clock(pos) + " / " + clock(dur) : clock(pos));
        c.volume.set(s.volume);
        c.remote.set(s.remote);
        c.locked.set(s.object != null && s.object.lockedByUser());
        c.glow.set(s.glowing);
        c.loop.set(s.loop);
        c.shared.set(s.shared);
        c.canShare.set(!s.remote && s.source != null && s.source.shareable());
        c.overlay.set(s.id.equals(video.overlayId));
        c.interactive.set(p != null && p.interactive());
        c.shape.set(Shapes.label(s.aspect()));
        boolean accept = video.status.needsAccept(s);
        boolean setup = !s.remote && video.status.missing(s) != null;
        c.needsAccept.set(accept);
        c.needsSetup.set(setup);
        String mode = c.mode.peek();
        if ((accept || setup) && mode.isEmpty()) c.switchMode(ScreenControls.GATE);
        else if (!accept && !setup && ScreenControls.GATE.equals(mode)) c.switchMode("");
    }

    static String clock(long ms) {
        long t = Math.max(0, ms / 1000);
        long hh = t / 3600, mm = t / 60 % 60, ss = t % 60;
        return hh > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", hh, mm, ss) : String.format(Locale.ROOT, "%d:%02d", mm, ss);
    }
}
