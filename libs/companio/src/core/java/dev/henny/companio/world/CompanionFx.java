package dev.henny.companio.world;

import dev.henny.companio.brain.Effects;
import dev.henny.companio.def.AnimRef;
import dev.henny.companio.def.ParticleDef;
import dev.henny.companio.def.SoundDef;

final class CompanionFx implements Effects {
    private final Companion c;

    CompanionFx(Companion c) {
        this.c = c;
    }

    public void anim(AnimRef anim, boolean untilEnd) {
        c.animator.play(anim, untilEnd);
    }

    public void stopAnim(String name) {
        c.animator.stop(name);
    }

    public void prop(String prop, boolean show) {
        c.setProp(prop, show);
    }

    public void sound(SoundDef sound) {
        float pitch = sound.pitchMin() + c.random.nextFloat() * (sound.pitchMax() - sound.pitchMin());
        c.output.sound(c, sound, pitch);
    }

    public void particle(ParticleDef particle) {
        c.output.particle(c, particle);
    }

    public void say(String text, float seconds) {
        if (!text.isBlank()) c.bubble.show(text, c.now, seconds);
    }

    public void hop(float height) {
        c.hopRequest = Math.max(c.hopRequest, height);
    }
}
