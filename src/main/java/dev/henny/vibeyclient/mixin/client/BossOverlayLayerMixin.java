package dev.henny.vibeyclient.mixin.client;

import dev.henny.vibeyclient.client.hud.VanillaLayers;
import dev.henny.vibeyclient.client.hud.VanillaLayers.Layer;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else
import net.minecraft.client.gui.GuiGraphics;

@Mixin(BossHealthOverlay.class)
public abstract class BossOverlayLayerMixin {
    //? if >=26.1 {
    /*@Shadow public abstract void extractRenderState(GuiGraphicsExtractor g);

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true, require = 0)
    private void vibeyclient$bossBar(GuiGraphicsExtractor g, CallbackInfo ci) {
        if (VanillaLayers.render(Layer.BOSS_BAR, g, () -> extractRenderState(g))) ci.cancel();
    }
    *///?} else {
    @Shadow public abstract void render(GuiGraphics g);

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void vibeyclient$bossBar(GuiGraphics g, CallbackInfo ci) {
        if (VanillaLayers.render(Layer.BOSS_BAR, g, () -> render(g))) ci.cancel();
    }
    //?}
}
