package dev.henny.cosmetico.anim;

import dev.henny.cosmetico.model.Track;

final class EmoteSample {
    private EmoteSample() {}

    static void track(Track t, float time, boolean loop, float[] pose, int o) {
        if (t == null) {
            pose[o] = pose[o + 1] = pose[o + 2] = pose[o + 3] = pose[o + 4] = pose[o + 5] = 0f;
            pose[o + 6] = pose[o + 7] = pose[o + 8] = 1f;
            return;
        }
        if (t.rotation() != null) Sampler.sample(t.rotation(), time, loop, pose, o);
        else pose[o] = pose[o + 1] = pose[o + 2] = 0f;
        if (t.position() != null) Sampler.sample(t.position(), time, loop, pose, o + 3);
        else pose[o + 3] = pose[o + 4] = pose[o + 5] = 0f;
        if (t.scale() != null) Sampler.sample(t.scale(), time, loop, pose, o + 6);
        else pose[o + 6] = pose[o + 7] = pose[o + 8] = 1f;
    }
}
