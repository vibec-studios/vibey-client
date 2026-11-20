package dev.henny.vibeyclient.module.visuals.mixin.effects;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.henny.vibeyclient.module.visuals.effects.Effects;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
abstract class LivingEntityFxMixin {
    @WrapWithCondition(method = "tickEffects", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private boolean vibey$ownPotion(Level level, ParticleOptions o, double x, double y, double z, double vx, double vy, double vz) {
        return !Effects.hideOwnPotionParticles() || (Object) this != Minecraft.getInstance().player;
    }
}
