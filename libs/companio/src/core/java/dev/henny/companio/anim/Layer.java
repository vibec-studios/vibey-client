package dev.henny.companio.anim;

import dev.henny.companio.def.Blend;
import dev.henny.cosmetico.model.Animation;

final class Layer {
    final Animation anim;
    final Blend blend;
    final float fade, speed;
    final boolean hold;
    float time, prevTime = -1f, weight;
    boolean stopping;

    Layer(Animation anim, Blend blend, float fade, float speed, boolean hold) {
        this.anim = anim;
        this.blend = blend;
        this.fade = Math.max(0.01f, fade);
        this.speed = speed;
        this.hold = hold;
    }

    boolean tick(float dt) {
        prevTime = time;
        time += dt * speed;
        if (anim.loop) {
            if (anim.length > 0 && time >= anim.length) time %= anim.length;
        } else if (time >= anim.length) {
            time = anim.length;
            if (!hold) stopping = true;
        }
        float step = dt / fade;
        weight = stopping ? weight - step : Math.min(1f, weight + step);
        return weight > 0f;
    }

    void stop() {
        stopping = true;
    }
}
