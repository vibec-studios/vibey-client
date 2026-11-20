package dev.henny.vibeyclient.module.visuals.mixin.effects;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.vibeyclient.module.visuals.effects.Effects;
import dev.henny.vibeyclient.module.visuals.effects.EffectsPreview;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
abstract class ScreenEffectRendererFxMixin {
    @ModifyArg(method = "renderFire", require = 0, index = 3, at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setColor(FFFF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"))
    private static float vibey$fireAlpha(float alpha) {
        return Effects.fireAlpha(alpha);
    }

    @ModifyExpressionValue(method = "renderScreenEffect", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isOnFire()Z"))
    private boolean vibey$previewFire(boolean burning) {
        return burning || EffectsPreview.fireOn();
    }

    @Inject(method = "renderItemActivationAnimation", require = 0, at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V", shift = At.Shift.AFTER))
    private void vibey$totemSize(PoseStack pose, float partial, SubmitNodeCollector collector, CallbackInfo ci) {
        float s = Effects.totemScale();
        if (s != 1f) pose.scale(s, s, s);
    }
}
