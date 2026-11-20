package dev.henny.vibeyclient.module.visuals.mixin;

import dev.henny.vibeyclient.module.visuals.particles.ParticleGate;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
abstract class ParticleEngineMixin {
    @Inject(method = "createParticle", require = 0, at = @At("HEAD"), cancellable = true)
    private void vibey$amount(ParticleOptions o, double x, double y, double z, double vx, double vy, double vz,
                              CallbackInfoReturnable<Particle> cir) {
        int n = ParticleGate.plan(o);
        if (n <= 0) {
            cir.setReturnValue(null);
            return;
        }
        if (n > 1) ParticleGate.spawnExtra((ParticleEngine) (Object) this, o, x, y, z, vx, vy, vz, n - 1);
        ParticleGate.enter();
    }

    @Inject(method = "createParticle", require = 0, at = @At("RETURN"))
    private void vibey$created(ParticleOptions o, double x, double y, double z, double vx, double vy, double vz,
                               CallbackInfoReturnable<Particle> cir) {
        ParticleGate.exit();
    }

    @Inject(method = "add", require = 0, at = @At("HEAD"), cancellable = true)
    private void vibey$direct(Particle particle, CallbackInfo ci) {
        if (ParticleGate.dropDirect(particle)) ci.cancel();
    }
}
