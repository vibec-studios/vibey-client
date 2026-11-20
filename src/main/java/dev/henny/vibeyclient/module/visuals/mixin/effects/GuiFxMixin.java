package dev.henny.vibeyclient.module.visuals.mixin.effects;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.henny.vibeyclient.module.visuals.effects.Effects;
import dev.henny.vibeyclient.module.visuals.effects.EffectsPreview;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
abstract class GuiFxMixin {
    @Unique
    private static final Identifier VIBEY_PUMPKIN = Identifier.withDefaultNamespace("textures/misc/pumpkinblur.png");
    private static final String OVERLAY = "Lnet/minecraft/client/gui/Gui;renderTextureOverlay(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/resources/Identifier;F)V";
    private static final String BLEND = "Lnet/minecraft/client/player/LocalPlayer;getEffectBlendFactor(Lnet/minecraft/core/Holder;F)F";

    @ModifyExpressionValue(method = "renderVignette", require = 0, at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/gui/Gui;vignetteBrightness:F"))
    private float vibey$vignette(float value) {
        return Effects.vignette(value);
    }

    @ModifyArg(method = "renderCameraOverlays", require = 0, index = 2, at = @At(value = "INVOKE", target = OVERLAY, ordinal = 0))
    private float vibey$pumpkin(float alpha) {
        return Effects.overlayAlpha(alpha);
    }

    @ModifyExpressionValue(method = "renderCameraOverlays", require = 0, at = @At(value = "INVOKE", target = BLEND))
    private float vibey$nausea(float value) {
        return Effects.nausea(value);
    }

    @ModifyExpressionValue(method = "renderCameraOverlays", require = 0, at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/player/LocalPlayer;portalEffectIntensity:F"))
    private float vibey$portal(float value) {
        return Effects.portal(value);
    }

    @ModifyExpressionValue(method = "renderCameraOverlays", require = 0, at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/player/LocalPlayer;oPortalEffectIntensity:F"))
    private float vibey$portalOld(float value) {
        return Effects.portal(value);
    }

    @Inject(method = "renderCameraOverlays", require = 0, at = @At("RETURN"))
    private void vibey$previewPumpkin(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        if (EffectsPreview.pumpkinOn()) ((GuiInvoker) this).vibey$textureOverlay(graphics, VIBEY_PUMPKIN, Effects.overlayAlpha(1f));
    }
}
