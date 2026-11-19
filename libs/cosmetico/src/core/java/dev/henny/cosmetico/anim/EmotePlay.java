package dev.henny.cosmetico.anim;

import dev.henny.cosmetico.model.Animation;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.PlayerBone;
import dev.henny.cosmetico.model.Track;

public final class EmotePlay {
    public static final float FADE = 0.15f;

    public final String id;
    public final Bundle bundle;
    public final Animation anim;
    public final double start;
    private double stopAt = Double.NaN;
    public float eventTime = -1f;
    public final float[] pose = new float[PlayerBone.VALUES.length * 9];
    public final int[] channels = new int[PlayerBone.VALUES.length];
    private final Track[] playerTracks = new Track[PlayerBone.VALUES.length];
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

    public boolean finished(double now) {
        return stopping() && now - stopAt >= FADE;
    }

    public void update(double now) {
        float elapsed = (float) (now - start);
        if (!anim.loop && !anim.hold && elapsed >= anim.length && !stopping()) stopAt = start + anim.length;
        localTime = Sampler.localTime(elapsed, anim.length, anim.loop);
        float in = Math.min(1f, Math.max(0f, elapsed / FADE));
        float out = stopping() ? Math.max(0f, 1f - (float) (now - stopAt) / FADE) : 1f;
        weight = smooth(in) * smooth(out);
        for (int i = 0; i < playerTracks.length; i++) EmoteSample.track(playerTracks[i], localTime, anim.loop, pose, i * 9);
    }

    private static float smooth(float x) {
        return x * x * (3f - 2f * x);
    }
}
