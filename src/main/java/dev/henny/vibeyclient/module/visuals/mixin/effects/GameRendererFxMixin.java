package dev.henny.vibeyclient.module.visuals.mixin.effects;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.vibeyclient.module.visuals.effects.Effects;
import dev.henny.vibeyclient.module.visuals.effects.ExplosionShake;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class GameRendererFxMixin {
    private static final String BLEND = "Lnet/minecraft/client/player/LocalPlayer;getEffectBlendFactor(Lnet/minecraft/core/Holder;F)F";

    @ModifyExpressionValue(method = "bobHurt", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"))
    private Object vibey$hurtTilt(Object value) {
        return value instanceof Double d ? Double.valueOf(Effects.hurtTilt(d)) : value;
    }

    @Inject(method = "bobHurt", require = 0, at = @At("RETURN"))
    private void vibey$blastShake(PoseStack pose, float partial, CallbackInfo ci) {
        ExplosionShake.apply(pose);
    }

    @ModifyExpressionValue(method = {"renderLevel", "tick"}, require = 0, at = @At(value = "INVOKE", target = BLEND))
    private float vibey$nausea(float value) {
        return Effects.nausea(value);
    }

    @ModifyExpressionValue(method = "renderLevel", require = 0, at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/player/LocalPlayer;portalEffectIntensity:F"))
    private float vibey$portal(float value) {
        return Effects.portal(value);
    }

    @ModifyExpressionValue(method = "renderLevel", require = 0, at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/player/LocalPlayer;oPortalEffectIntensity:F"))
    private float vibey$portalOld(float value) {
        return Effects.portal(value);
    }
}
