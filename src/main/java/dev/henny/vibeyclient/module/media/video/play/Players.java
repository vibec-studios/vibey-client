package dev.henny.vibeyclient.module.media.video.play;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.media.ffmpeg.Ffmpeg;
import dev.henny.vibeyclient.media.player.FfmpegPlayer;
import dev.henny.vibeyclient.media.player.MediaPlayer;
import dev.henny.vibeyclient.module.media.video.MediaScreen;
import dev.henny.vibeyclient.module.media.video.Video;
import dev.henny.vibeyclient.module.media.video.web.Web;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Players {
    public static final int MAX_ACTIVE = 6;
    private static final long CLOSE_AFTER_MS = 15_000;
    private static final double DRAW_DISTANCE = 160;

    private final Video video;
    private final Follower follower;
    private final List<MediaScreen> wanted = new ArrayList<>();

    public Players(Video video) {
        this.video = video;
        this.follower = new Follower(video);
    }

    public Follower follower() {
        return follower;
    }

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        Vec3 eye = mc.player.getEyePosition();
        String dim = video.dimension();
        long now = System.currentTimeMillis();
        int range = video.cfg.distance.get();
        wanted.clear();
        for (MediaScreen s : video.screens.values()) {
            boolean here = s.placed() && dim.equals(s.dimension());
            s.distance = here ? eye.distanceTo(s.center()) : Double.MAX_VALUE;
            boolean overlay = s.id.equals(video.overlayId);
            if (s.source != null && allowed(s) && (overlay || here && s.distance <= range)) wanted.add(s);
        }
        wanted.sort(Comparator.comparingDouble(s -> s.id.equals(video.overlayId) ? -1 : s.distance + (s.playing() ? 0 : 10_000)));
        for (int i = 0; i < wanted.size(); i++) {
            MediaScreen s = wanted.get(i);
            if (i >= MAX_ACTIVE) {
                park(s, now);
                continue;
            }
            s.farSince = 0;
            if (s.player == null) s.player = create(s);
            if (s.player != null) follower.follow(s, range);
        }
        for (MediaScreen s : video.screens.values()) if (!wanted.contains(s) && s.player != null) park(s, now);
    }

    public void frame() {
        for (MediaScreen s : video.screens.values()) {
            if (s.player == null) continue;
            boolean overlay = s.id.equals(video.overlayId) && !ClientCompat.hudHidden();
            boolean inWorld = s.placed() && s.distance < DRAW_DISTANCE && s.object.state().visible();
            try {
                s.player.frame(overlay || inWorld);
            } catch (RuntimeException e) {
                Vibey.LOG.warn("Video frame failed: {}", e.toString());
                close(s);
            }
        }
    }

    public boolean allowed(MediaScreen s) {
        if (!s.remote) return true;
        return switch (video.cfg.others.get()) {
            case NEVER -> false;
            case ALWAYS -> true;
            case ASK -> s.accepted;
        };
    }

    private void park(MediaScreen s, long now) {
        if (s.player == null) return;
        s.player.pause();
        s.player.audio(0f, null);
        follower.forget(s.id);
        if (s.farSince == 0) s.farSince = now;
        else if (now - s.farSince > CLOSE_AFTER_MS) close(s);
    }

    private MediaPlayer create(MediaScreen s) {
        try {
            if (s.source.kind().web()) {
                if (Web.state(video.status.browserConsent()) == Web.State.STARTING) Web.start();
                if (Web.state(video.status.browserConsent()) != Web.State.READY) return null;
                int[] px = PixelSize.of(video.cfg.quality.get().lines, s.aspect(), true);
                follower.forget(s.id);
                return Web.create(s.source, px[0], px[1], s.expectedMs(), s.playing());
            }
            String ff = Ffmpeg.find();
            if (ff == null) return null;
            int[] px = PixelSize.of(video.cfg.quality.get().lines, s.aspect(), false);
            follower.applied(s.id, s.playing());
            return new FfmpegPlayer(ff, s.source.uri(), px[0], px[1], video.cfg.fps.get(), s.expectedMs(), s.playing());
        } catch (RuntimeException | LinkageError e) {
            Vibey.LOG.warn("Could not start a player for {}: {}", s.source.label(), e.toString());
            return null;
        }
    }

    public void close(MediaScreen s) {
        if (s.player != null) {
            try {
                s.player.close();
            } catch (RuntimeException e) {
                Vibey.LOG.debug("Closing player: {}", e.toString());
            }
        }
        s.player = null;
        s.started = false;
        s.farSince = 0;
        follower.forget(s.id);
    }

    public void closeAll() {
        for (MediaScreen s : video.screens.values()) close(s);
    }
}
