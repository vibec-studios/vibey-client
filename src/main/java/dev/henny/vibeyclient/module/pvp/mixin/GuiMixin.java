package dev.henny.vibeyclient.module.pvp.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.henny.vibeyclient.module.pvp.PvpHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if >=26.3 {
/*import com.mojang.renderpearl.api.pipeline.RenderPipeline;
*///?} else if >=1.21.6 {
import com.mojang.blaze3d.pipeline.RenderPipeline;
//?}

/**
 * Custom crosshair: replaces only the crosshair sprite inside vanilla's {@code renderCrosshair} ({@code extractCrosshair}
 * from 26.1), so vanilla still decides when it shows (F1, third person, spectator, debug crosshair) and the attack
 * indicator stays.
 *
 * <p>{@code @WrapOperation} (not {@code @Redirect}) so it stacks with other crosshair mods that redirect or wrap the
 * same call (Centered Crosshair, …); {@code require = 0} so a mod that removes the call can never crash the game:
 * the module page then says the custom crosshair is not available ({@code PvpModule.notices}).
 */
//? if >=26.2 {
/*@Mixin(net.minecraft.client.gui.Hud.class)
*///?} else
@Mixin(net.minecraft.client.gui.Gui.class)
abstract class GuiMixin {
    //? if >=26.1 {
    /*@WrapOperation(method = "extractCrosshair", require = 0, at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private void vibey$crosshair(net.minecraft.client.gui.GuiGraphicsExtractor g, RenderPipeline pipeline,
                                 net.minecraft.resources.Identifier sprite, int x, int y, int w, int h, Operation<Void> original) {
        if (!PvpHooks.crosshair(g)) original.call(g, pipeline, sprite, x, y, w, h);
    }
    *///?} else if >=1.21.11 {
    @WrapOperation(method = "renderCrosshair", require = 0, at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    private void vibey$crosshair(net.minecraft.client.gui.GuiGraphics g, RenderPipeline pipeline,
                                 net.minecraft.resources.Identifier sprite, int x, int y, int w, int h, Operation<Void> original) {
        if (!PvpHooks.crosshair(g)) original.call(g, pipeline, sprite, x, y, w, h);
    }
    //?} else if >=1.21.6 {
    /*@WrapOperation(method = "renderCrosshair", require = 0, at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private void vibey$crosshair(net.minecraft.client.gui.GuiGraphics g, RenderPipeline pipeline,
                                 net.minecraft.resources.ResourceLocation sprite, int x, int y, int w, int h, Operation<Void> original) {
        if (!PvpHooks.crosshair(g)) original.call(g, pipeline, sprite, x, y, w, h);
    }
    *///?} else if >=1.21.2 {
    /*@WrapOperation(method = "renderCrosshair", require = 0, at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Ljava/util/function/Function;Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private void vibey$crosshair(net.minecraft.client.gui.GuiGraphics g, java.util.function.Function<?, ?> type,
                                 net.minecraft.resources.ResourceLocation sprite, int x, int y, int w, int h, Operation<Void> original) {
        if (!PvpHooks.crosshair(g)) original.call(g, type, sprite, x, y, w, h);
    }
    *///?} else {
    /*@WrapOperation(method = "renderCrosshair", require = 0, at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private void vibey$crosshair(net.minecraft.client.gui.GuiGraphics g, net.minecraft.resources.ResourceLocation sprite,
                                 int x, int y, int w, int h, Operation<Void> original) {
        if (!PvpHooks.crosshair(g)) original.call(g, sprite, x, y, w, h);
    }
    *///?}
}
