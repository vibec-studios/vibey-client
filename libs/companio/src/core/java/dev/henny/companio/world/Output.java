package dev.henny.companio.world;

import dev.henny.companio.def.ParticleDef;
import dev.henny.companio.def.SoundDef;

public interface Output {
    void sound(Companion c, SoundDef sound, float pitch);

    void particle(Companion c, ParticleDef particle);

    Output NONE = new Output() {
        public void sound(Companion c, SoundDef sound, float pitch) {}
        public void particle(Companion c, ParticleDef particle) {}
    };
}
