package dev.henny.cosmetico.mc.mixin;

import dev.henny.cosmetico.mc.render.WorldPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=1.21.6 {
@Mixin(net.minecraft.client.gui.render.pip.GuiEntityRenderer.class)
//?} else
/*@Mixin(net.minecraft.client.gui.screens.inventory.InventoryScreen.class)*/
public abstract class GuiEntityMixin {
    //? if >=26.2 {
    /*private static final String TARGET = "renderToTexture(Lnet/minecraft/client/renderer/state/gui/pip/GuiEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V";
    *///?} else if >=26.1 {
    /*private static final String TARGET = "renderToTexture(Lnet/minecraft/client/renderer/state/gui/pip/GuiEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V";
    *///?} else if >=1.21.6 {
    private static final String TARGET = "renderToTexture(Lnet/minecraft/client/gui/render/state/pip/GuiEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V";
    //?} else
    /*private static final String TARGET = "renderEntityInInventory(Lnet/minecraft/client/gui/GuiGraphics;FFFLorg/joml/Vector3f;Lorg/joml/Quaternionf;Lorg/joml/Quaternionf;Lnet/minecraft/world/entity/LivingEntity;)V";*/

    //? if >=1.21.6 {
    @Inject(method = TARGET, at = @At("HEAD"))
    private void cosmetico$guiStart(CallbackInfo ci) {
        dev.henny.cosmetico.mc.render.GuiDepth.enter();
    }

    @Inject(method = TARGET, at = @At("RETURN"))
    private void cosmetico$guiEnd(CallbackInfo ci) {
        dev.henny.cosmetico.mc.render.GuiDepth.leave();
    }
    //?} else {
    /*@Inject(method = TARGET, at = @At("HEAD"))
    private static void cosmetico$guiStart(CallbackInfo ci) {
        dev.henny.cosmetico.mc.render.GuiDepth.enter();
    }

    @Inject(method = TARGET, at = @At("RETURN"))
    private static void cosmetico$guiEnd(CallbackInfo ci) {
        dev.henny.cosmetico.mc.render.GuiDepth.leave();
    }
    *///?}
}
