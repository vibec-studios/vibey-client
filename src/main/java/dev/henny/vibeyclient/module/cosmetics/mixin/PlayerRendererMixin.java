package dev.henny.vibeyclient.module.cosmetics.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.vibeyclient.avatar.AvatarState;
import dev.henny.vibeyclient.avatar.render.AvatarCarrier;
import dev.henny.vibeyclient.avatar.render.AvatarHooks;
import dev.henny.vibeyclient.avatar.render.CosmeticLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=1.21.9 {
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
//?} else if >=1.21.2 {
/*import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
*///?} else {
/*import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
*///?}

/**
 * Adds the cosmetic layer, snapshots the player into the render state and applies the emote's root bone. Up to
 * 1.21.8 the class is PlayerRenderer, from 1.21.9 AvatarRenderer (players and mannequins).
 */
//? if >=1.21.9 {
@Mixin(AvatarRenderer.class)
//?} else
/*@Mixin(PlayerRenderer.class)*/
public abstract class PlayerRendererMixin {
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "<init>", at = @At("TAIL"))
    private void vibey$addLayer(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
        ((LivingEntityRendererAccessor) this).vibey$layers().add(new CosmeticLayer((LivingEntityRenderer) (Object) this));
    }

    //? if >=1.21.9 {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void vibey$extract(Avatar avatar, AvatarRenderState state, float partial, CallbackInfo ci) {
        AvatarState st = avatar instanceof Player p ? AvatarHooks.extract(p, partial) : null;
        ((AvatarCarrier) state).vibey$setAvatar(st);
        if (st != null && AvatarHooks.hideHeldItems(st)) {
            state.rightHandItemState.clear();
            state.leftHandItemState.clear();
            state.rightHandItemStack = net.minecraft.world.item.ItemStack.EMPTY;
            state.leftHandItemStack = net.minecraft.world.item.ItemStack.EMPTY;
            state.rightArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
            state.leftArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
        }
    }

    @Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V", at = @At("TAIL"))
    private void vibey$root(AvatarRenderState state, PoseStack ps, float bodyRot, float scale, CallbackInfo ci) {
        AvatarHooks.rootTransform(((AvatarCarrier) state).vibey$avatar(), ps, 0.9375f);
    }
    //?} else if >=1.21.2 {
    /*@Inject(method = "extractRenderState(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;F)V", at = @At("TAIL"))
    private void vibey$extract(AbstractClientPlayer player, PlayerRenderState state, float partial, CallbackInfo ci) {
        AvatarState st = AvatarHooks.extract(player, partial);
        ((AvatarCarrier) state).vibey$setAvatar(st);
        if (st != null && AvatarHooks.hideHeldItems(st)) {
            //? if >=1.21.4 {
            state.rightHandItem.clear();
            state.leftHandItem.clear();
            //?} else {
            /^state.rightHandItem = net.minecraft.world.item.ItemStack.EMPTY;
            state.leftHandItem = net.minecraft.world.item.ItemStack.EMPTY;
            ^///?}
            state.rightArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
            state.leftArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
        }
    }

    @Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V", at = @At("TAIL"))
    private void vibey$root(PlayerRenderState state, PoseStack ps, float bodyRot, float scale, CallbackInfo ci) {
        AvatarHooks.rootTransform(((AvatarCarrier) state).vibey$avatar(), ps, 0.9375f);
    }
    *///?} else {
    /*@Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    private void vibey$render(AbstractClientPlayer player, float yaw, float partial, PoseStack ps, MultiBufferSource buffers, int light, CallbackInfo ci) {
        dev.henny.vibeyclient.avatar.render.WorldPass.render121(player, partial, ps, buffers, light);
    }

    @Inject(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V", at = @At("TAIL"))
    private void vibey$root(AbstractClientPlayer player, PoseStack ps, float bob, float bodyRot, float partial, float scale, CallbackInfo ci) {
        AvatarHooks.rootTransform(dev.henny.vibeyclient.avatar.AvatarStates.peek(player.getUUID()), ps, 0.9375f);
    }
    *///?}
}
