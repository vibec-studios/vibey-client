package dev.henny.vibeyclient.module.visuals.mixin.effects;

import dev.henny.vibeyclient.module.visuals.effects.ExplosionShake;
import dev.henny.vibeyclient.module.visuals.particles.ParticleGate;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ParticleEngine.class, priority = 900)
abstract class ParticleEngineFxMixin {
    @Inject(method = "createParticle", require = 0, at = @At("HEAD"))
    private void vibey$blast(ParticleOptions o, double x, double y, double z, double vx, double vy, double vz,
                             CallbackInfoReturnable<Particle> cir) {
        ExplosionShake.seen(o, x, y, z);
    }

    @Inject(method = "createParticle", require = 0, at = @At("RETURN"))
    private void vibey$style(ParticleOptions o, double x, double y, double z, double vx, double vy, double vz,
                             CallbackInfoReturnable<Particle> cir) {
        ParticleGate.tag(cir.getReturnValue(), o);
    }
}
