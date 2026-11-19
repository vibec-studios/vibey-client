package dev.henny.vibeyclient.module.pvp.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.henny.vibeyclient.module.pvp.PvpHooks;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Custom crosshair: replaces only the crosshair sprite inside vanilla's {@code renderCrosshair}, so vanilla still
 * decides when it shows (F1, third person, spectator, debug crosshair) and the attack indicator stays.
 */
@Mixin(Gui.class)
abstract class GuiMixin {
    @Redirect(method = "renderCrosshair", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private void vibey$crosshair(GuiGraphics g, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h) {
        if (!PvpHooks.crosshair(g)) g.blitSprite(pipeline, sprite, x, y, w, h);
    }
}
