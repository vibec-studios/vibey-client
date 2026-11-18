package dev.henny.vibeyclient.module.cosmetics.mixin;

import dev.henny.vibeyclient.avatar.render.WorldPass;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Marks vanilla's GUI entity rendering (inventory player, ...) so the world pass skips pets/auras/particles there:
 * they live in world space. Before 1.21.6 that is InventoryScreen.renderEntityInInventory, later the entity
 * picture-in-picture renderer.
 */
//? if >=1.21.6 {
@Mixin(net.minecraft.client.gui.render.pip.GuiEntityRenderer.class)
//?} else
/*@Mixin(net.minecraft.client.gui.screens.inventory.InventoryScreen.class)*/
public abstract class GuiEntityMixin {
    //? if >=26.1 {
    /*private static final String TARGET = "renderToTexture(Lnet/minecraft/client/renderer/state/gui/pip/GuiEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V";
    *///?} else if >=1.21.6 {
    private static final String TARGET = "renderToTexture(Lnet/minecraft/client/gui/render/state/pip/GuiEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V";
    //?} else
    /*private static final String TARGET = "renderEntityInInventory(Lnet/minecraft/client/gui/GuiGraphics;FFFLorg/joml/Vector3f;Lorg/joml/Quaternionf;Lorg/joml/Quaternionf;Lnet/minecraft/world/entity/LivingEntity;)V";*/

    //? if >=1.21.6 {
    @Inject(method = TARGET, at = @At("HEAD"))
    private void vibey$guiStart(CallbackInfo ci) {
        WorldPass.gui++;
    }

    @Inject(method = TARGET, at = @At("RETURN"))
    private void vibey$guiEnd(CallbackInfo ci) {
        WorldPass.gui = Math.max(0, WorldPass.gui - 1);
    }
    //?} else {
    /*@Inject(method = TARGET, at = @At("HEAD"))
    private static void vibey$guiStart(CallbackInfo ci) {
        WorldPass.gui++;
    }

    @Inject(method = TARGET, at = @At("RETURN"))
    private static void vibey$guiEnd(CallbackInfo ci) {
        WorldPass.gui = Math.max(0, WorldPass.gui - 1);
    }
    *///?}
}
