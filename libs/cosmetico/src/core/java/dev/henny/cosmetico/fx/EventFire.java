package dev.henny.cosmetico.fx;

import dev.henny.cosmetico.config.CosmeticoConfig;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Event;
import dev.henny.cosmetico.state.Avatar;

final class EventFire {
    private final double[] pos = new double[3];
    private final float[] vel = new float[3], spread = new float[3];

    void fire(Avatar a, Bundle b, Event ev, FxSink sink, CosmeticoConfig config) {
        switch (ev.type) {
            case PARTICLE -> particle(a, b, ev, sink);
            case SOUND -> sound(a, b, ev, sink, config);
            default -> {}
        }
    }

    private void particle(Avatar a, Bundle b, Event ev, FxSink sink) {
        FxMath.position(a, ev.locator, pos);
        FxMath.toWorld(a.frame.bodyYaw, ev.velocity[0], ev.velocity[1], ev.velocity[2], vel);
        FxMath.toWorld(a.frame.bodyYaw, ev.spread[0], ev.spread[1], ev.spread[2], spread);
        float sx = Math.abs(spread[0]), sy = Math.abs(spread[1]), sz = Math.abs(spread[2]);
        if (ev.sprite >= 0) {
            sink.sprite(a.uuid, b, new SpriteSpawn(ev.sprite, pos[0], pos[1], pos[2], vel[0], vel[1], vel[2], sx, sy, sz, ev.count,
                    ev.lifetime, ev.size, ev.gravity, ev.emissive));
            return;
        }
        sink.particle(ev.id, pos[0], pos[1], pos[2], vel[0], vel[1], vel[2], ev.count, sx, sy, sz);
    }

    private void sound(Avatar a, Bundle b, Event ev, FxSink sink, CosmeticoConfig config) {
        FxMath.position(a, ev.locator, pos);
        float vol = ev.volume * config.emoteVolume;
        if (vol <= 0f) return;
        if (ev.sound >= 0) sink.embedded(b.sounds[ev.sound], pos[0], pos[1], pos[2], vol, ev.pitch);
        else sink.sound(ev.id, pos[0], pos[1], pos[2], vol, ev.pitch);
    }
}
