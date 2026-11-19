package dev.henny.vibeyclient.module.pvp.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.henny.vibeyclient.module.pvp.PvpHooks;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Custom crosshair: replaces only the crosshair sprite inside vanilla's {@code renderCrosshair}, so vanilla still
 * decides when it shows (F1, third person, spectator, debug crosshair) and the attack indicator stays.
 *
 * <p>{@code @WrapOperation} (not {@code @Redirect}) so it stacks with other crosshair mods that redirect or wrap the
 * same call (Centered Crosshair, …); {@code require = 0} so a mod that removes the call can never crash the game:
 * the module page then says the custom crosshair is not available ({@code PvpModule.notices}).
 */
@Mixin(Gui.class)
abstract class GuiMixin {
    @WrapOperation(method = "renderCrosshair", require = 0, at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private void vibey$crosshair(GuiGraphics g, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h,
                                 Operation<Void> original) {
        if (!PvpHooks.crosshair(g)) original.call(g, pipeline, sprite, x, y, w, h);
    }
}
