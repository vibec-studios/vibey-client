package dev.henny.vibeyclient.module.visuals.mixin.effects;

import dev.henny.vibeyclient.module.visuals.particles.StyleSlot;
import dev.henny.vibeyclient.module.visuals.particles.StyledParticle;
import net.minecraft.client.particle.SingleQuadParticle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SingleQuadParticle.class)
abstract class SingleQuadParticleMixin {
    private static final String QUAD = "extractRotatedQuad(Lnet/minecraft/client/renderer/state/QuadParticleRenderState;Lorg/joml/Quaternionf;FFFF)V";
    private static final String ADD = "Lnet/minecraft/client/renderer/state/QuadParticleRenderState;add(Lnet/minecraft/client/particle/SingleQuadParticle$Layer;FFFFFFFFFFFFII)V";

    @ModifyArg(method = QUAD, require = 0, at = @At(value = "INVOKE", target = ADD), index = 8)
    private float vibey$size(float size) {
        StyleSlot s = ((StyledParticle) this).vibey$slot();
        return s == null ? size : s.size(size);
    }

    @ModifyArg(method = QUAD, require = 0, at = @At(value = "INVOKE", target = ADD), index = 13)
    private int vibey$tint(int argb) {
        StyleSlot s = ((StyledParticle) this).vibey$slot();
        return s == null ? argb : s.color(argb);
    }
}
