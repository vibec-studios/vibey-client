package dev.henny.vibeyclient.avatar.golden;

import dev.henny.vibeyclient.avatar.anim.Sampler;
import dev.henny.vibeyclient.avatar.bundle.Bundle;
import dev.henny.vibeyclient.avatar.bundle.Bundle.Channel;
import dev.henny.vibeyclient.avatar.render.AvatarDraw;

final class AnimDump {
    private AnimDump() {}

    static final double[] NOWS = {0.3, 7.77, 123.456};

    static String dump(Bundle b) throws Exception {
        Out o = new Out();
        for (Bundle.Animation a : b.animations.values()) {
            o.line("animation", a.name, a.length, a.loop);
            for (double now : NOWS) o.line("time", now, AvatarDraw.time(a, now));
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

    private static void samples(Out o, int track, String ch, Channel c, Bundle.Animation a) {
        if (c == null) return;
        float[] v = new float[3];
        for (int k = 0; k <= 10; k++) {
            float t = -0.05f + k * (a.length + 0.1f) / 10f;
            Sampler.sample(c, t, a.loop, v, 0);
            o.line("sample", track, ch, t, v[0], v[1], v[2]);
        }
    }
}
