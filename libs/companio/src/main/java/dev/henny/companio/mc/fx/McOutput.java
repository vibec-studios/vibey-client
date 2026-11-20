package dev.henny.companio.mc.fx;

import dev.henny.companio.config.CompanioConfig;
import dev.henny.companio.def.ParticleDef;
import dev.henny.companio.def.SoundDef;
import dev.henny.companio.mc.render.ViewLocators;
import dev.henny.companio.pack.PackPaths;
import dev.henny.companio.world.Companion;
import dev.henny.companio.world.CompanionView;
import dev.henny.companio.world.Output;
import dev.henny.cosmetico.fx.FxMath;
import dev.henny.cosmetico.mc.fx.VanillaFx;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Sound;

import java.util.function.Supplier;

public final class McOutput implements Output {
    private final Supplier<CompanioConfig> config;
    private final float[] vel = new float[3], spread = new float[3];

    public McOutput(Supplier<CompanioConfig> config) {
        this.config = config;
    }

    @Override
    public void sound(Companion c, SoundDef s, float pitch) {
        float volume = s.volume() * config.get().volume;
        if (volume <= 0.001f) return;
        double x = c.body.x, y = c.body.y + c.body.height * 0.5, z = c.body.z;
        if (s.embedded()) {
            Sound snd = find(c.model(), PackPaths.fileName(s.file()));
            if (snd != null) VanillaFx.INSTANCE.embedded(snd, x, y, z, volume, pitch);
        } else {
            VanillaFx.INSTANCE.sound(s.event(), x, y, z, volume, pitch);
        }
    }

    @Override
    public void particle(Companion c, ParticleDef p) {
        particle(c, p.particle(), p.locator(), p.count(), p.spread(), p.velocity());
    }

    public void particle(CompanionView v, String id, String locator, int count, float[] spreadIn, float[] velocity) {
        double[] at = ViewLocators.get(v, locator);
        double x = at != null ? at[0] : v.body().x, y = at != null ? at[1] : v.body().y + v.body().height * 0.5, z = at != null ? at[2] : v.body().z;
        float yaw = v.body().yaw;
        FxMath.toWorld(yaw, velocity[0], velocity[1], velocity[2], vel);
        FxMath.toWorld(yaw, spreadIn[0], spreadIn[1], spreadIn[2], spread);
        VanillaFx.INSTANCE.particle(id, x, y, z, vel[0], vel[1], vel[2], count, Math.abs(spread[0]), Math.abs(spread[1]), Math.abs(spread[2]));
    }

    static Sound find(Bundle b, String id) {
        if (b == null || b.sounds == null) return null;
        for (Sound s : b.sounds) if (s.id().equals(id)) return s;
        return null;
    }
}
