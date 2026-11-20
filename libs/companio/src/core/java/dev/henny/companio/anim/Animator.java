package dev.henny.companio.anim;

import dev.henny.companio.def.AnimRef;
import dev.henny.companio.def.Blend;
import dev.henny.companio.pack.PackLimits;
import dev.henny.cosmetico.model.Animation;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.skeleton.Mix;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class Animator {
    private static final float BASE_FADE = 0.2f;

    private Bundle bundle;
    private Animation base, prevBase;
    private float baseTime, prevBaseTime, baseEventFrom = -1f, prevWeight;
    private final List<Layer> layers = new ArrayList<>();

    public Animator(Bundle bundle) {
        this.bundle = bundle;
    }

    public void bundle(Bundle b) {
        if (b == bundle) return;
        bundle = b;
        String name = base == null ? null : base.name;
        base = prevBase = null;
        layers.clear();
        if (name != null) base(name, 1f, 0f);
    }

    public void base(String name, float speed, float dt) {
        Animation a = name == null ? null : bundle.animation(name);
        if (a != base) {
            prevBase = base;
            prevBaseTime = baseTime;
            prevWeight = base == null ? 0f : 1f;
            base = a;
            baseTime = 0f;
            baseEventFrom = -1f;
        } else {
            baseEventFrom = baseTime;
        }
        if (base != null) {
            baseTime += dt * speed;
            if (base.length > 0 && baseTime >= base.length) baseTime = base.loop ? baseTime % base.length : base.length;
        }
        prevWeight = Math.max(0f, prevWeight - dt / BASE_FADE);
        if (prevWeight <= 0f) prevBase = null;
    }

    public void play(AnimRef ref, boolean hold) {
        Animation a = bundle.animation(ref.name());
        if (a == null) return;
        for (Layer l : layers) if (l.anim == a && !l.stopping) return;
        if (layers.size() >= PackLimits.MAX_LAYERS) layers.get(0).stop();
        layers.add(new Layer(a, ref.blend(), ref.fade(), ref.speed(), hold));
    }

    public void stop(String name) {
        for (Layer l : layers) if (l.anim.name.equals(name)) l.stop();
    }

    public void tick(float dt) {
        layers.removeIf(l -> !l.tick(dt));
        while (layers.size() > PackLimits.MAX_LAYERS + 2) layers.remove(0);
    }

    public void fill(Mix mix) {
        mix.clear();
        float replace = 0f;
        for (Layer l : layers) if (l.blend == Blend.REPLACE) replace = Math.max(replace, l.weight);
        float baseScale = 1f - replace;
        float cur = 1f - prevWeight;
        if (base != null) mix.add(base, baseTime, cur * baseScale);
        if (prevBase != null) mix.add(prevBase, prevBaseTime, prevWeight * baseScale);
        for (Layer l : layers) mix.add(l.anim, l.time, l.weight);
        Animation vis = strongestWithVisibility(cur * baseScale);
        mix.visibility = vis;
        mix.visibilityTime = vis == base ? baseTime : layerTime(vis);
    }

    private Animation strongestWithVisibility(float baseWeight) {
        Animation best = base != null && base.hasVisibility ? base : null;
        float bw = best == null ? 0f : baseWeight;
        for (Layer l : layers) if (l.anim.hasVisibility && l.weight >= bw) {
            best = l.anim;
            bw = l.weight;
        }
        if (best == null) {
            Animation idle = bundle.animation("idle");
            if (idle != null && idle.hasVisibility && idle == base) best = idle;
        }
        return best;
    }

    private float layerTime(Animation a) {
        for (Layer l : layers) if (l.anim == a) return l.time;
        return 0f;
    }

    public void events(EventSink sink) {
        if (base != null && 1f - prevWeight >= 0.5f) sink.window(base, baseEventFrom, baseTime);
        for (Layer l : layers) if (l.weight >= 0.5f || (!l.stopping && l.prevTime < 0.05f)) sink.window(l.anim, l.prevTime, l.time);
    }

    public void layers(Consumer<String> names) {
        for (Layer l : layers) if (!l.stopping) names.accept(l.anim.name);
    }

    public String baseName() {
        return base == null ? null : base.name;
    }

    public interface EventSink {
        void window(Animation anim, float from, float to);
    }
}
