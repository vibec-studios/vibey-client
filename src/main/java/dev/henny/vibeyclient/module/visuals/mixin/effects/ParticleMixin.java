package dev.henny.vibeyclient.module.visuals.mixin.effects;

import dev.henny.vibeyclient.module.visuals.particles.StyleSlot;
import dev.henny.vibeyclient.module.visuals.particles.StyledParticle;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Particle.class)
abstract class ParticleMixin implements StyledParticle {
    @Unique
    private StyleSlot vibey$styleSlot;

    @Override
    public StyleSlot vibey$slot() {
        return vibey$styleSlot;
    }

    @Override
    public void vibey$slot(StyleSlot slot) {
        vibey$styleSlot = slot;
    }
}
