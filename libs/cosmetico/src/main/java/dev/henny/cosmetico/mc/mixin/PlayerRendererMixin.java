package dev.henny.cosmetico.mc.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.cosmetico.mc.render.AvatarCarrier;
import dev.henny.cosmetico.mc.player.EmotePoseHook;
import dev.henny.cosmetico.mc.player.PlayerSnapshot;
import dev.henny.cosmetico.mc.player.RootPoseHook;
import dev.henny.cosmetico.mc.player.CapeShieldTextures;
import dev.henny.cosmetico.mc.render.CosmeticLayer;
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

//? if >=1.21.9 {
@Mixin(AvatarRenderer.class)
//?} else
/*@Mixin(PlayerRenderer.class)*/
public abstract class PlayerRendererMixin {
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "<init>", at = @At("TAIL"))
    private void cosmetico$addLayer(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
        ((LivingEntityRendererAccessor) this).cosmetico$layers().add(new CosmeticLayer((LivingEntityRenderer) (Object) this));
    }

    //? if >=1.21.9 {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void cosmetico$extract(Avatar avatar, AvatarRenderState state, float partial, CallbackInfo ci) {
        dev.henny.cosmetico.state.Avatar st = avatar instanceof Player p ? PlayerSnapshot.extract(p, partial) : null;
        ((AvatarCarrier) state).cosmetico$setAvatar(st);
        if (st != null && CapeShieldTextures.cape(st) != null) state.showCape = true;
        if (st != null && RootPoseHook.hideHeldItems(st)) {
            //? if >=1.21.11 {
            state.rightHandItemState.clear();
            state.leftHandItemState.clear();
            state.rightHandItemStack = net.minecraft.world.item.ItemStack.EMPTY;
            state.leftHandItemStack = net.minecraft.world.item.ItemStack.EMPTY;
            //?} else {
            /*state.rightHandItem.clear();
            state.leftHandItem.clear();
            *///?}
            state.rightArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
            state.leftArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
        }
    }

    @Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V", at = @At("TAIL"))
    private void cosmetico$root(AvatarRenderState state, PoseStack ps, float bodyRot, float scale, CallbackInfo ci) {
        RootPoseHook.apply(((AvatarCarrier) state).cosmetico$avatar(), ps, 0.9375f);
    }
    //?} else if >=1.21.2 {
    /*@Inject(method = "extractRenderState(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;F)V", at = @At("TAIL"))
    private void cosmetico$extract(AbstractClientPlayer player, PlayerRenderState state, float partial, CallbackInfo ci) {
        dev.henny.cosmetico.state.Avatar st = PlayerSnapshot.extract(player, partial);
        ((AvatarCarrier) state).cosmetico$setAvatar(st);
        if (st != null && CapeShieldTextures.cape(st) != null) state.showCape = true;
        if (st != null && RootPoseHook.hideHeldItems(st)) {
            //? if >=1.21.4 {
            state.rightHandItem.clear();
            state.leftHandItem.clear();
            state.rightArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
            state.leftArmPose = net.minecraft.client.model.HumanoidModel.ArmPose.EMPTY;
            //?} else {
            /^state.rightHandItem = net.minecraft.world.item.ItemStack.EMPTY;
            state.leftHandItem = net.minecraft.world.item.ItemStack.EMPTY;
            state.rightHandItemModel = null;
            state.leftHandItemModel = null;
            ^///?}
        }
    }

    @Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V", at = @At("TAIL"))
    private void cosmetico$root(PlayerRenderState state, PoseStack ps, float bodyRot, float scale, CallbackInfo ci) {
        RootPoseHook.apply(((AvatarCarrier) state).cosmetico$avatar(), ps, 0.9375f);
    }
    *///?} else {
    /*@Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    private void cosmetico$render(AbstractClientPlayer player, float yaw, float partial, PoseStack ps, MultiBufferSource buffers, int light, CallbackInfo ci) {
        dev.henny.cosmetico.mc.render.WorldPass.render121(player, partial, ps, buffers, light);
    }

    @Inject(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V", at = @At("TAIL"))
    private void cosmetico$root(AbstractClientPlayer player, PoseStack ps, float bob, float bodyRot, float partial, float scale, CallbackInfo ci) {
        RootPoseHook.apply(dev.henny.cosmetico.Cosmetico.engine().avatars.peek(player.getUUID()), ps, 0.9375f);
    }
    *///?}
}
