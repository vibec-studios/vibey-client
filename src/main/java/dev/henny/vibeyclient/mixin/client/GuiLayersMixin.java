package dev.henny.vibeyclient.mixin.client;

import dev.henny.vibeyclient.client.hud.VanillaLayers;
import dev.henny.vibeyclient.client.hud.VanillaLayers.Layer;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else
import net.minecraft.client.gui.GuiGraphics;

//? if >=26.2 {
/*@Mixin(net.minecraft.client.gui.Hud.class)
*///?} else
@Mixin(net.minecraft.client.gui.Gui.class)
public abstract class GuiLayersMixin {
    //? if >=26.1 {
    /*@Shadow private void extractEffects(GuiGraphicsExtractor g, DeltaTracker tick) {}
    @Shadow private void extractTitle(GuiGraphicsExtractor g, DeltaTracker tick) {}
    @Shadow private void extractScoreboardSidebar(GuiGraphicsExtractor g, DeltaTracker tick) {}
    @Shadow private void extractTabList(GuiGraphicsExtractor g, DeltaTracker tick) {}

    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true, require = 0)
    private void vibeyclient$effects(GuiGraphicsExtractor g, DeltaTracker tick, CallbackInfo ci) {
        if (VanillaLayers.render(Layer.EFFECTS, g, () -> extractEffects(g, tick))) ci.cancel();
    }

    @Inject(method = "extractTitle", at = @At("HEAD"), cancellable = true, require = 0)
    private void vibeyclient$title(GuiGraphicsExtractor g, DeltaTracker tick, CallbackInfo ci) {
        if (VanillaLayers.render(Layer.TITLE, g, () -> extractTitle(g, tick))) ci.cancel();
    }

    @Inject(method = "extractScoreboardSidebar", at = @At("HEAD"), cancellable = true, require = 0)
    private void vibeyclient$scoreboard(GuiGraphicsExtractor g, DeltaTracker tick, CallbackInfo ci) {
        if (VanillaLayers.render(Layer.SCOREBOARD, g, () -> extractScoreboardSidebar(g, tick))) ci.cancel();
    }

    @Inject(method = "extractTabList", at = @At("HEAD"), cancellable = true, require = 0)
    private void vibeyclient$tabList(GuiGraphicsExtractor g, DeltaTracker tick, CallbackInfo ci) {
        if (VanillaLayers.render(Layer.PLAYER_LIST, g, () -> extractTabList(g, tick))) ci.cancel();
    }
    *///?} else {
    @Shadow private void renderEffects(GuiGraphics g, DeltaTracker tick) {}
    @Shadow private void renderTitle(GuiGraphics g, DeltaTracker tick) {}
    @Shadow private void renderScoreboardSidebar(GuiGraphics g, DeltaTracker tick) {}
    @Shadow private void renderTabList(GuiGraphics g, DeltaTracker tick) {}

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true, require = 0)
    private void vibeyclient$effects(GuiGraphics g, DeltaTracker tick, CallbackInfo ci) {
        if (VanillaLayers.render(Layer.EFFECTS, g, () -> renderEffects(g, tick))) ci.cancel();
    }

    @Inject(method = "renderTitle", at = @At("HEAD"), cancellable = true, require = 0)
    private void vibeyclient$title(GuiGraphics g, DeltaTracker tick, CallbackInfo ci) {
        if (VanillaLayers.render(Layer.TITLE, g, () -> renderTitle(g, tick))) ci.cancel();
    }

    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true, require = 0)
    private void vibeyclient$scoreboard(GuiGraphics g, DeltaTracker tick, CallbackInfo ci) {
        if (VanillaLayers.render(Layer.SCOREBOARD, g, () -> renderScoreboardSidebar(g, tick))) ci.cancel();
    }

    @Inject(method = "renderTabList", at = @At("HEAD"), cancellable = true, require = 0)
    private void vibeyclient$tabList(GuiGraphics g, DeltaTracker tick, CallbackInfo ci) {
        if (VanillaLayers.render(Layer.PLAYER_LIST, g, () -> renderTabList(g, tick))) ci.cancel();
    }
    //?}
}
