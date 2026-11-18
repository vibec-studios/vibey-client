package dev.henny.vibeyclient.avatar.anim;

import dev.henny.vibeyclient.avatar.bundle.Bundle;
import dev.henny.vibeyclient.avatar.bundle.Bundle.Animation;
import dev.henny.vibeyclient.avatar.bundle.Bundle.PlayerBone;
import dev.henny.vibeyclient.avatar.bundle.Bundle.Track;

/**
 * One emote being played on one avatar: timing (0.15 s fade in/out, loop/hold/once) and the sampled pose of the
 * seven player bones in Blockbench units (degrees / pixels / factors, offsets from the standing pose).
 */
public final class EmotePlay {
    public static final float FADE = 0.15f;

    public final String id;
    public final Bundle bundle;
    public final Animation anim;
    public final double start;
    /** When stopping began (fade out), NaN while playing. */
    private double stopAt = Double.NaN;
    /** Last animation-local time events were processed up to (for the event runner). */
    public float eventTime = -1f;
    public int loops;

    /** 7 player bones × (rot xyz, pos xyz, scale xyz). */
    public final float[] pose = new float[PlayerBone.VALUES.length * 9];
    /** Which player bones the emote animates (rotation/position/scale bits). */
    public final int[] channels = new int[PlayerBone.VALUES.length];
    private final Track[] playerTracks = new Track[PlayerBone.VALUES.length];
    private final float[] tmp = new float[3];
    public float weight;
    public float localTime;

    public EmotePlay(String id, Bundle bundle, double start) {
        this.id = id;
        this.bundle = bundle;
        this.anim = bundle.emoteAnimation();
        this.start = start;
        for (PlayerBone b : PlayerBone.VALUES) {
            Track t = anim.track(b);
            playerTracks[b.ordinal()] = t;
            if (t != null) channels[b.ordinal()] = (t.rotation() != null ? 1 : 0) | (t.position() != null ? 2 : 0) | (t.scale() != null ? 4 : 0);
        }
    }

    public boolean loops() {
        return anim.loop;
    }

    public void stop(double now) {
        if (Double.isNaN(stopAt)) stopAt = now;
    }

    public boolean stopping() {
        return !Double.isNaN(stopAt);
    }

    /** True once the fade out finished (remove it). */
    public boolean finished(double now) {
        return stopping() && now - stopAt >= FADE;
    }

    /** Updates {@link #localTime}, {@link #weight} and {@link #pose} for {@code now} (seconds). */
    public void update(double now) {
        float elapsed = (float) (now - start);
        if (!anim.loop && !anim.hold && elapsed >= anim.length && !stopping()) stopAt = start + anim.length;
        localTime = Sampler.localTime(elapsed, anim.length, anim.loop);
        float in = Math.min(1f, Math.max(0f, elapsed / FADE));
        float out = stopping() ? Math.max(0f, 1f - (float) (now - stopAt) / FADE) : 1f;
        weight = smooth(in) * smooth(out);
        for (int i = 0; i < playerTracks.length; i++) {
            Track t = playerTracks[i];
            int o = i * 9;
            if (t == null) {
                pose[o] = pose[o + 1] = pose[o + 2] = pose[o + 3] = pose[o + 4] = pose[o + 5] = 0f;
                pose[o + 6] = pose[o + 7] = pose[o + 8] = 1f;
                continue;
            }
            if (t.rotation() != null) Sampler.sample(t.rotation(), localTime, anim.loop, pose, o);
            else pose[o] = pose[o + 1] = pose[o + 2] = 0f;
            if (t.position() != null) Sampler.sample(t.position(), localTime, anim.loop, pose, o + 3);
            else pose[o + 3] = pose[o + 4] = pose[o + 5] = 0f;
            if (t.scale() != null) Sampler.sample(t.scale(), localTime, anim.loop, pose, o + 6);
            else pose[o + 6] = pose[o + 7] = pose[o + 8] = 1f;
        }
    }

    private static float smooth(float x) {
        return x * x * (3f - 2f * x);
    }
}
