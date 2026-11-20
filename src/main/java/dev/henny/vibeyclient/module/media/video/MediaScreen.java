package dev.henny.vibeyclient.module.media.video;

import dev.henny.r3.object.Transform;
import dev.henny.r3.object.WorldObject;
import dev.henny.vibeyclient.media.player.MediaPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class MediaScreen {
    public static final float DEFAULT_WIDTH = 3f;

    public final UUID id;
    public UUID owner;
    public String ownerName = "";
    public boolean remote;
    public boolean accepted;

    public MediaSource source;
    public boolean glowing = true;
    public boolean loop = true;
    public float volume = 1f;
    public boolean shared = true;
    public float aspect = 16f / 9f;

    public final Timeline timeline = new Timeline();
    public WorldObject object;

    public transient MediaPlayer player;
    public transient long farSince;
    public transient boolean started;
    public transient boolean correcting;
    public transient long lagMs;
    public transient double distance = Double.MAX_VALUE;

    public MediaScreen(UUID id) {
        this.id = id;
    }

    public boolean placed() {
        return object != null;
    }

    public String dimension() {
        return object == null ? null : object.dimension();
    }

    public Vec3 center() {
        if (object == null) return Vec3.ZERO;
        var p = object.transform().position();
        return new Vec3(p.x, p.y, p.z);
    }

    public float aspect() {
        if (object == null) return aspect;
        Transform t = object.transform();
        return t.height() <= 0 ? aspect : t.width() / t.height();
    }

    public float worldWidth() {
        return object == null ? DEFAULT_WIDTH : object.transform().worldWidth();
    }

    public boolean playing() {
        return timeline.playing();
    }

    public long expectedMs() {
        return timeline.expectedMs(loop);
    }

    public void setTimeline(boolean playing, long positionMs) {
        timeline.set(playing, positionMs);
    }
}
