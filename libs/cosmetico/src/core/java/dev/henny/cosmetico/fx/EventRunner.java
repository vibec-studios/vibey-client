package dev.henny.cosmetico.fx;

import dev.henny.cosmetico.anim.AnimTime;
import dev.henny.cosmetico.anim.EmotePlay;
import dev.henny.cosmetico.anim.StateWeights;
import dev.henny.cosmetico.config.CosmeticoConfig;
import dev.henny.cosmetico.draw.ShownBundle;
import dev.henny.cosmetico.draw.StateWeightsInput;
import dev.henny.cosmetico.host.Log;
import dev.henny.cosmetico.model.Animation;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Slot;
import dev.henny.cosmetico.state.Avatar;
import dev.henny.cosmetico.state.EventClocks;

import java.util.function.Supplier;

public final class EventRunner {
    private final ShownBundle shown;
    private final CosmeticoConfig config;
    private final Supplier<FxSink> sink;
    private final Supplier<Log> log;
    private final StateWeightsInput weights = new StateWeightsInput();
    private final float[] w = new float[StateWeights.COUNT];
    private final EventFire fire = new EventFire();

    public EventRunner(ShownBundle shown, CosmeticoConfig config, Supplier<FxSink> sink, Supplier<Log> log) {
        this.shown = shown;
        this.config = config;
        this.sink = sink;
        this.log = log;
    }

    public void tick(Avatar a, double now) {
        EmotePlay e = a.emote;
        if (e != null && config.emotes && !config.emotesServerOff && !e.stopping()) {
            float t = e.localTime;
            run(a, e.bundle, e.anim, e.eventTime, t);
            e.eventTime = t;
        }
        if (a.frame.hidden) return;
        for (Slot slot : Slot.all()) {
            Bundle b = shown.bundle(a, slot);
            if (b == null) a.events.reset(slot);
            else cosmetic(a, b, slot, now);
        }
    }

    private void cosmetic(Avatar a, Bundle b, Slot slot, double now) {
        int si = slot.ordinal();
        Animation idle = b.animation("idle");
        if (idle != null && idle.events.length > 0) {
            float t = AnimTime.global(idle, now);
            float last = a.events.idle[si];
            if (last >= 0f) run(a, b, idle, last, t);
            a.events.idle[si] = t;
        }
        weights.weights(a, b, slot, w);
        for (int k = 0; k < EventClocks.MOVE_ANIMS; k++) {
            Animation anim = b.animation(StateWeights.NAMES[k + 1]);
            int i = si * EventClocks.MOVE_ANIMS + k;
            if (anim == null || anim.events.length == 0 || w[k + 1] < 0.5f) {
                a.events.move[i] = -1f;
                continue;
            }
            float t = AnimTime.state(a.motion.stateStart, k + 1, anim, now);
            float last = a.events.move[i];
            if (last >= 0f) run(a, b, anim, last, t);
            a.events.move[i] = t;
        }
    }

    private void run(Avatar a, Bundle b, Animation anim, float from, float to) {
        EventWindow.run(anim, from, to, anim.loop, ev -> {
            try {
                fire.fire(a, b, ev, sink.get(), config);
            } catch (RuntimeException ex) {
                log.get().debug("Event of " + b.id + " failed: " + ex);
            }
        });
    }
}
