package dev.henny.cosmetico.mc.mixin;

import dev.henny.cosmetico.mc.render.AvatarCarrier;
import dev.henny.cosmetico.mc.player.ShieldDraw;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {
    //? if >=1.21.9 {
    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;FF)V", at = @At("HEAD"))
    private void cosmetico$owner(com.mojang.blaze3d.vertex.PoseStack ps, net.minecraft.client.renderer.SubmitNodeCollector out, int light,
                             net.minecraft.client.renderer.entity.state.ArmedEntityRenderState state, float y, float x, CallbackInfo ci) {
        ShieldDraw.owner = state instanceof AvatarCarrier c ? c.cosmetico$avatar() : null;
    }

    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;FF)V", at = @At("RETURN"))
    private void cosmetico$ownerEnd(CallbackInfo ci) {
        ShieldDraw.owner = null;
    }
    //?} else if >=1.21.4 {
    /*@Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;FF)V", at = @At("HEAD"))
    private void cosmetico$owner(com.mojang.blaze3d.vertex.PoseStack ps, net.minecraft.client.renderer.MultiBufferSource out, int light,
                             net.minecraft.client.renderer.entity.state.ArmedEntityRenderState state, float y, float x, CallbackInfo ci) {
        ShieldDraw.owner = state instanceof AvatarCarrier c ? c.cosmetico$avatar() : null;
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;FF)V", at = @At("RETURN"))
    private void cosmetico$ownerEnd(CallbackInfo ci) {
        ShieldDraw.owner = null;
    }
    *///?} else if >=1.21.2 {
    /*@Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V", at = @At("HEAD"))
    private void cosmetico$owner(com.mojang.blaze3d.vertex.PoseStack ps, net.minecraft.client.renderer.MultiBufferSource out, int light,
                             net.minecraft.client.renderer.entity.state.LivingEntityRenderState state, float y, float x, CallbackInfo ci) {
        ShieldDraw.owner = state instanceof AvatarCarrier c ? c.cosmetico$avatar() : null;
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V", at = @At("RETURN"))
    private void cosmetico$ownerEnd(CallbackInfo ci) {
        ShieldDraw.owner = null;
    }
    *///?} else {
    /*@Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", at = @At("HEAD"), cancellable = true)
    private void cosmetico$owner(com.mojang.blaze3d.vertex.PoseStack ps, net.minecraft.client.renderer.MultiBufferSource out, int light,
                             net.minecraft.world.entity.LivingEntity entity, float a, float b, float c, float d, float e, float f, CallbackInfo ci) {
        ShieldDraw.owner = entity instanceof net.minecraft.world.entity.player.Player p ? dev.henny.cosmetico.Cosmetico.engine().avatars.peek(p.getUUID()) : null;
        if (ShieldDraw.owner != null && dev.henny.cosmetico.mc.player.RootPoseHook.hideHeldItems(ShieldDraw.owner)) {
            ShieldDraw.owner = null;
            ci.cancel();
        }
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V", at = @At("RETURN"))
    private void cosmetico$ownerEnd(CallbackInfo ci) {
        ShieldDraw.owner = null;
    }
    *///?}
}
