package dev.henny.companio.brain;

import dev.henny.companio.def.AnimRef;
import dev.henny.companio.def.ParticleDef;
import dev.henny.companio.def.SoundDef;

public interface Effects {
    void anim(AnimRef anim, boolean untilEnd);

    void stopAnim(String name);

    void prop(String prop, boolean show);

    void sound(SoundDef sound);

    void particle(ParticleDef particle);

    void say(String text, float seconds);

    void hop(float height);

    Effects NONE = new Effects() {
        public void anim(AnimRef anim, boolean untilEnd) {}
        public void stopAnim(String name) {}
        public void prop(String prop, boolean show) {}
        public void sound(SoundDef sound) {}
        public void particle(ParticleDef particle) {}
        public void say(String text, float seconds) {}
        public void hop(float height) {}
    };
}
