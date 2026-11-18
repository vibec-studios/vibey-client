package dev.henny.vibeyclient.module.cosmetics.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
//? if >=1.21.2 {
import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.vibeyclient.avatar.render.AvatarCarrier;
import dev.henny.vibeyclient.avatar.render.WorldPass;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//?}

/** World-attached cosmetics (pets, auras) and sprite particles, drawn at the player's feet before the model. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    //? if >=1.21.9 {
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("HEAD"))
    private void vibey$world(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state, PoseStack ps,
                             net.minecraft.client.renderer.SubmitNodeCollector out, net.minecraft.client.renderer.state.CameraRenderState camera, CallbackInfo ci) {
        if (state instanceof AvatarCarrier c && c.vibey$avatar() != null) WorldPass.submit(c.vibey$avatar(), state, ps, out, camera);
    }
    //?} else if >=1.21.2 {
    /*@Inject(method = "render(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    private void vibey$world(net.minecraft.client.renderer.entity.state.LivingEntityRenderState state, PoseStack ps,
                             net.minecraft.client.renderer.MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (state instanceof AvatarCarrier c && c.vibey$avatar() != null) WorldPass.render(c.vibey$avatar(), state, ps, buffers, light);
    }
    *///?}
}
