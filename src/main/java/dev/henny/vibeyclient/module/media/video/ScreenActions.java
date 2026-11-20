package dev.henny.vibeyclient.module.media.video;

import dev.henny.r3.interact.Interaction;
import dev.henny.r3.mc.R3Mc;
import dev.henny.r3.object.Transform;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.media.player.MediaPlayer;
import dev.henny.vibeyclient.module.media.video.controls.ScreenControls;
import dev.henny.vibeyclient.module.media.video.world.ScreenObjects;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.UUID;

public final class ScreenActions {
    private static final long SKIP_MS = 10_000;

    private final Video video;

    ScreenActions(Video video) {
        this.video = video;
    }

    public void screensKey() {
        if (!video.inWorld()) return;
        Interaction interaction = R3Mc.scene().interaction();
        if (video.find(interaction.held()) != null) {
            interaction.cancel();
            return;
        }
        MediaScreen looked = lookedAt();
        if (looked != null) {
            ScreenControls c = ScreenControls.of(looked);
            if (c != null) c.openMore();
            return;
        }
        MediaScreen overlayOnly = video.overlayScreen();
        if (overlayOnly != null && !overlayOnly.placed()) place(overlayOnly);
        else newScreen(null, 16f / 9f);
    }

    public MediaScreen lookedAt() {
        if (!video.inWorld()) return null;
        return video.find(R3Mc.scene().interaction().hover().target());
    }

    public MediaScreen newScreen(MediaSource source, float aspect) {
        MediaScreen s = own(source, aspect);
        place(s);
        return s;
    }

    public MediaScreen overlayOnly(MediaSource source, float aspect) {
        MediaScreen s = own(source, aspect);
        video.screens.put(s.id, s);
        video.overlayId = s.id;
        return s;
    }

    private MediaScreen own(MediaSource source, float aspect) {
        Minecraft mc = Minecraft.getInstance();
        MediaScreen s = new MediaScreen(UUID.randomUUID());
        s.owner = mc.player != null ? mc.player.getUUID() : null;
        s.ownerName = ClientCompat.playerName();
        s.source = source;
        s.aspect = aspect;
        s.loop = source == null || source.kind() == MediaSource.Kind.FILE;
        s.setTimeline(true, source == null ? 0 : source.youTubeStartSeconds() * 1000L);
        return s;
    }

    public void place(MediaScreen s) {
        if (!s.placed()) ScreenObjects.create(s, video.dimension());
        video.screens.put(s.id, s);
        R3Mc.place(s.object);
    }

    public void placed(MediaScreen s) {
        video.screens.put(s.id, s);
        changed(s);
        if (s.source == null) {
            ScreenControls c = ScreenControls.of(s);
            if (c != null) c.editSource();
        }
    }

    public void changed(MediaScreen s) {
        if (s.remote) return;
        if (s.object != null) s.object.fullBright(s.glowing);
        video.store.save();
        video.net.sendScreen(s);
    }

    public void setSource(MediaScreen s, MediaSource source) {
        s.source = source;
        s.timeline.durationMs(-1);
        s.loop = source.kind() == MediaSource.Kind.FILE || s.loop;
        s.setTimeline(true, source.youTubeStartSeconds() * 1000L);
        video.players.close(s);
        changed(s);
    }

    public void setShape(MediaScreen s, float aspect) {
        s.aspect = aspect;
        if (s.object != null) {
            Transform t = s.object.transform();
            t.size(t.width(), t.width() / aspect);
            ScreenObjects.fitLayout(s.object);
        }
        video.players.close(s);
        changed(s);
    }

    public void setPlaying(MediaScreen s, boolean playing) {
        MediaPlayer p = s.player;
        long pos = p != null && p.status() != MediaPlayer.Status.LOADING ? p.positionMs() : s.expectedMs();
        s.setTimeline(playing, pos);
        if (!s.remote) video.net.sendState(s);
    }

    public void seek(MediaScreen s, long ms) {
        s.setTimeline(s.playing(), Math.max(0, ms));
        if (s.player != null) s.player.seek(Math.max(0, ms));
        video.players.follower().corrected(s.id);
        if (!s.remote) video.net.sendState(s);
    }

    public void skip(MediaScreen s, int direction) {
        seek(s, s.expectedMs() + direction * SKIP_MS);
    }

    public void stop(MediaScreen s) {
        s.setTimeline(false, 0);
        if (s.player != null) {
            s.player.pause();
            s.player.seek(0);
        }
        video.players.follower().forget(s.id);
        if (!s.remote) video.net.sendState(s);
    }

    public void toggleLock(MediaScreen s) {
        if (s.object == null || s.remote) return;
        s.object.locked(!s.object.lockedByUser());
        changed(s);
    }

    public void toggleOverlay(MediaScreen s) {
        video.overlayId = s.id.equals(video.overlayId) ? null : s.id;
        if (video.overlayId == null && !s.placed()) remove(s);
    }

    public void unload(MediaScreen s) {
        s.accepted = false;
        video.players.close(s);
    }

    public void remove(MediaScreen s) {
        video.players.close(s);
        video.unplace(s);
        video.screens.remove(s.id);
        if (s.id.equals(video.overlayId)) video.overlayId = null;
        if (s.remote) return;
        video.net.sendRemove(s);
        video.store.save();
    }

    public void removeAllOwn() {
        for (MediaScreen s : new ArrayList<>(video.screens.values())) if (!s.remote) remove(s);
    }
}
