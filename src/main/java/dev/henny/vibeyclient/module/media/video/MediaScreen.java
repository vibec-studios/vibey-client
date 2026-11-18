package dev.henny.vibeyclient.module.media.video;

import com.google.gson.JsonObject;
import dev.henny.vibeyclient.module.media.video.player.MediaPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * One screen: where it hangs, what it plays and the shared playback timeline. The timeline is the truth (owner's
 * view): {@link #expectedMs()} says where playback should be now; the local {@link #player} follows it.
 * A screen with {@code dimension == null} is not placed in the world (overlay only).
 */
public final class MediaScreen {
    public final UUID id;
    public UUID owner;
    public String ownerName = "";
    public boolean remote;

    public String dimension;
    public double x, y, z;
    /** The side that shows the picture (points away from the wall). */
    public Direction normal = Direction.NORTH;
    /** Picture "up": UP for walls, a horizontal direction for floor/ceiling screens. */
    public Direction up = Direction.UP;
    public float width = 3f, height = 1.6875f;

    public MediaSource source;
    public boolean glowing = true;
    public boolean loop = true;
    /** Own volume of this screen, 0..1 (times the module volume). */
    public float volume = 1f;
    /** Own screens: send to other players (only if the source is shareable). */
    public boolean shared = true;

    // Timeline
    public boolean playing;
    public long baseMs;
    public long baseWall;
    /** Duration when known (ms), else -1. */
    public long durationMs = -1;

    // Local state
    /** Remote screens in "ask" mode: the user agreed to load it. */
    public boolean accepted;
    public transient MediaPlayer player;
    public transient long farSince;
    public transient boolean visible;
    public transient double distance = Double.MAX_VALUE;

    public MediaScreen(UUID id) {
        this.id = id;
    }

    public boolean placed() {
        return dimension != null;
    }

    public long expectedMs() {
        long t = playing ? baseMs + (System.currentTimeMillis() - baseWall) : baseMs;
        if (playing && loop && durationMs > 1000) t %= durationMs;
        return Math.max(0, t);
    }

    public void setTimeline(boolean playing, long positionMs) {
        this.playing = playing;
        this.baseMs = Math.max(0, positionMs);
        this.baseWall = System.currentTimeMillis();
    }

    public float aspect() {
        return height <= 0 ? 16f / 9f : width / height;
    }

    public Vec3 center() {
        return new Vec3(x, y, z);
    }

    /** Right vector of the picture (as seen by a viewer in front of it). */
    public Vec3 right() {
        Vec3 f = new Vec3(-normal.getStepX(), -normal.getStepY(), -normal.getStepZ());
        Vec3 u = upVec();
        return f.cross(u);
    }

    public Vec3 upVec() {
        return new Vec3(up.getStepX(), up.getStepY(), up.getStepZ());
    }

    public AABB bounds() {
        Vec3 r = right().scale(width / 2);
        Vec3 u = upVec().scale(height / 2);
        Vec3 c = center();
        Vec3 a = c.add(r).add(u);
        Vec3 b = c.subtract(r).subtract(u);
        return new AABB(a, b).inflate(0.05);
    }

    /**
     * Where the ray from {@code eye} along {@code dir} hits the picture: {x, y} in 0..1 from the top left, or null.
     */
    public double[] hit(Vec3 eye, Vec3 dir, double maxDist) {
        Vec3 n = new Vec3(normal.getStepX(), normal.getStepY(), normal.getStepZ());
        double denom = dir.dot(n);
        if (denom >= -1e-6) return null; // looking at the back or parallel
        double t = center().subtract(eye).dot(n) / denom;
        if (t < 0 || t > maxDist) return null;
        Vec3 p = eye.add(dir.scale(t)).subtract(center());
        double u = p.dot(right()) / width + 0.5;
        double v = 0.5 - p.dot(upVec()) / height;
        if (u < 0 || u > 1 || v < 0 || v > 1) return null;
        return new double[] {u, v, t};
    }

    // ============================================================================================ persistence

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("id", id.toString());
        if (dimension != null) {
            o.addProperty("dim", dimension);
            o.addProperty("x", x);
            o.addProperty("y", y);
            o.addProperty("z", z);
            o.addProperty("normal", normal.getSerializedName());
            o.addProperty("up", up.getSerializedName());
        }
        o.addProperty("w", width);
        o.addProperty("h", height);
        if (source != null) {
            o.addProperty("kind", source.kind().name());
            o.addProperty("uri", source.uri());
        }
        o.addProperty("glow", glowing);
        o.addProperty("loop", loop);
        o.addProperty("vol", volume);
        o.addProperty("shared", shared);
        o.addProperty("pos", expectedMs());
        return o;
    }

    public static MediaScreen fromJson(JsonObject o) {
        MediaScreen s = new MediaScreen(UUID.fromString(o.get("id").getAsString()));
        if (o.has("dim")) {
            s.dimension = o.get("dim").getAsString();
            s.x = o.get("x").getAsDouble();
            s.y = o.get("y").getAsDouble();
            s.z = o.get("z").getAsDouble();
            Direction n = Direction.byName(o.get("normal").getAsString());
            Direction u = Direction.byName(o.get("up").getAsString());
            if (n != null) s.normal = n;
            if (u != null) s.up = u;
        }
        s.width = Math.clamp(o.get("w").getAsFloat(), 0.25f, 32f);
        s.height = Math.clamp(o.get("h").getAsFloat(), 0.25f, 32f);
        if (o.has("kind") && o.has("uri")) {
            try {
                s.source = new MediaSource(MediaSource.Kind.valueOf(o.get("kind").getAsString()), o.get("uri").getAsString());
            } catch (IllegalArgumentException ignored) {
                // unknown kind from a newer version
            }
        }
        if (o.has("glow")) s.glowing = o.get("glow").getAsBoolean();
        if (o.has("loop")) s.loop = o.get("loop").getAsBoolean();
        if (o.has("vol")) s.volume = Math.clamp(o.get("vol").getAsFloat(), 0f, 1f);
        if (o.has("shared")) s.shared = o.get("shared").getAsBoolean();
        s.setTimeline(false, o.has("pos") ? o.get("pos").getAsLong() : 0);
        return s;
    }
}
