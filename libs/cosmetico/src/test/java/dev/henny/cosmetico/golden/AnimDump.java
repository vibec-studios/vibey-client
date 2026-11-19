package dev.henny.cosmetico.golden;

import dev.henny.cosmetico.anim.AnimTime;
import dev.henny.cosmetico.anim.Sampler;
import dev.henny.cosmetico.model.Animation;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Channel;

public final class AnimDump {
    private AnimDump() {}

    static final double[] NOWS = {0.3, 7.77, 123.456};

    public static String dump(Bundle b) {
        Out o = new Out();
        for (Animation a : b.animations.values()) {
            o.line("animation", a.name, a.length, a.loop);
            for (double now : NOWS) o.line("time", now, AnimTime.global(a, now));
            for (float el : new float[]{-0.5f, 0.2f, a.length * 1.5f, a.length * 3.25f}) {
                o.line("local", el, Sampler.localTime(el, a.length, false), Sampler.localTime(el, a.length, true));
            }
            for (int k = 0; k < a.tracks.length; k++) {
                samples(o, k, "rotation", a.tracks[k].rotation(), a);
                samples(o, k, "position", a.tracks[k].position(), a);
                samples(o, k, "scale", a.tracks[k].scale(), a);
            }
        }
        if (b.isEmote()) EmoteDump.dump(o, b);
        return o.toString();
    }

    private static void samples(Out o, int track, String ch, Channel c, Animation a) {
        if (c == null) return;
        float[] v = new float[3];
        for (int k = 0; k <= 10; k++) {
            float t = -0.05f + k * (a.length + 0.1f) / 10f;
            Sampler.sample(c, t, a.loop, v, 0);
            o.line("sample", track, ch, t, v[0], v[1], v[2]);
        }
    }
}
