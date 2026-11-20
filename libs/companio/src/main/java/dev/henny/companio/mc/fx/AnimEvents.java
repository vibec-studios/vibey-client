package dev.henny.companio.mc.fx;

import dev.henny.companio.config.CompanioConfig;
import dev.henny.companio.mc.render.ViewLocators;
import dev.henny.companio.world.CompanionView;
import dev.henny.cosmetico.fx.EventWindow;
import dev.henny.cosmetico.mc.fx.VanillaFx;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Event;
import dev.henny.cosmetico.model.EventType;
import dev.henny.cosmetico.model.Locator;

import java.util.function.Supplier;

public final class AnimEvents {
    private final McOutput output;
    private final Supplier<CompanioConfig> config;

    public AnimEvents(McOutput output, Supplier<CompanioConfig> config) {
        this.output = output;
        this.config = config;
    }

    public void fire(CompanionView v, boolean sounds) {
        Bundle b = v.model();
        if (b == null) return;
        v.animator().events((anim, from, to) -> EventWindow.run(anim, from, to, anim.loop, ev -> one(v, b, ev, sounds)));
    }

    private void one(CompanionView v, Bundle b, Event ev, boolean sounds) {
        if (ev.type == EventType.PARTICLE && ev.sprite < 0) {
            output.particle(v, ev.id, locator(b, ev.locator), ev.count, ev.spread, ev.velocity);
        } else if (ev.type == EventType.SOUND && sounds) {
            float volume = ev.volume * config.get().volume;
            if (volume <= 0.001f) return;
            double[] at = ViewLocators.get(v, locator(b, ev.locator));
            double x = at != null ? at[0] : v.body().x, y = at != null ? at[1] : v.body().y + 0.3, z = at != null ? at[2] : v.body().z;
            if (ev.sound >= 0) VanillaFx.INSTANCE.embedded(b.sounds[ev.sound], x, y, z, volume, ev.pitch);
            else VanillaFx.INSTANCE.sound(ev.id, x, y, z, volume, ev.pitch);
        }
    }

    private static String locator(Bundle b, Locator l) {
        return l == null ? null : l.name();
    }
}
