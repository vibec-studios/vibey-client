package dev.henny.vibeyclient.module.cosmetics.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.vibeyclient.avatar.render.ShieldDraw;
import dev.henny.vibeyclient.avatar.render.Tex;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=1.21.11 {
import net.minecraft.client.model.object.equipment.ShieldModel;
//?} else
/*import net.minecraft.client.model.ShieldModel;*/

/** Shield cosmetic texture: replaces vanilla's shield draw while a player with a shield cosmetic is drawn. */
//? if >=1.21.4 {
@Mixin(net.minecraft.client.renderer.special.ShieldSpecialRenderer.class)
//?} else
/*@Mixin(net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer.class)*/
public abstract class ShieldRendererMixin {
    //? if >=1.21.4 {
    @Shadow
    private ShieldModel model;
    //?} else {
    /*@Shadow
    private ShieldModel shieldModel;
    *///?}

    //? if >=26.1 {
    /*@Inject(method = "submit(Lnet/minecraft/core/component/DataComponentMap;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIZI)V", at = @At("HEAD"), cancellable = true)
    private void vibey$shield(net.minecraft.core.component.DataComponentMap data, PoseStack ps, net.minecraft.client.renderer.SubmitNodeCollector out,
                              int light, int overlay, boolean foil, int outline, CallbackInfo ci) {
        Tex t = ShieldDraw.texture();
        if (t == null) return;
        ShieldDraw.draw(model.handle(), model.plate(), ps, out, t, light, overlay);
        ci.cancel();
    }
    *///?} else if >=1.21.9 {
    @Inject(method = "submit(Lnet/minecraft/core/component/DataComponentMap;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIZI)V", at = @At("HEAD"), cancellable = true)
    private void vibey$shield(net.minecraft.core.component.DataComponentMap data, net.minecraft.world.item.ItemDisplayContext ctx, PoseStack ps,
                              net.minecraft.client.renderer.SubmitNodeCollector out, int light, int overlay, boolean foil, int outline, CallbackInfo ci) {
        Tex t = ShieldDraw.texture();
        if (t == null) return;
        ShieldDraw.draw(model.handle(), model.plate(), ps, out, t, light, overlay);
        ci.cancel();
    }
    //?} else if >=1.21.4 {
    /*@Inject(method = "render(Lnet/minecraft/core/component/DataComponentMap;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IIZ)V", at = @At("HEAD"), cancellable = true)
    private void vibey$shield(net.minecraft.core.component.DataComponentMap data, net.minecraft.world.item.ItemDisplayContext ctx, PoseStack ps,
                              net.minecraft.client.renderer.MultiBufferSource buffers, int light, int overlay, boolean foil, CallbackInfo ci) {
        Tex t = ShieldDraw.texture();
        if (t == null) return;
        ShieldDraw.draw(model.handle(), model.plate(), ps, buffers, t, light, overlay);
        ci.cancel();
    }
    *///?} else {
    /*@Inject(method = "renderByItem", at = @At("HEAD"), cancellable = true)
    private void vibey$shield(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.ItemDisplayContext ctx, PoseStack ps,
                              net.minecraft.client.renderer.MultiBufferSource buffers, int light, int overlay, CallbackInfo ci) {
        if (!stack.is(net.minecraft.world.item.Items.SHIELD)) return;
        Tex t = ShieldDraw.texture();
        if (t == null) return;
        ShieldDraw.draw(shieldModel.handle(), shieldModel.plate(), ps, buffers, t, light, overlay);
        ci.cancel();
    }
    *///?}
}
