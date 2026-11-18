package dev.henny.vibeyclient.module.emotes.mixin;

import dev.henny.vibeyclient.avatar.render.AvatarHooks;
import net.minecraft.client.model.HumanoidModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=1.21.11 {
import net.minecraft.client.model.player.PlayerModel;
//?} else
/*import net.minecraft.client.model.PlayerModel;*/

/** Emote pose on top of vanilla's: after the player model's setupAnim. */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    //? if >=1.21.9 {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void vibey$emote(net.minecraft.client.renderer.entity.state.AvatarRenderState state, CallbackInfo ci) {
        HumanoidModel<?> m = (HumanoidModel<?>) (Object) this;
        AvatarHooks.emotePose(m.head, m.body, m.rightArm, m.leftArm, m.rightLeg, m.leftLeg,
                ((dev.henny.vibeyclient.avatar.render.AvatarCarrier) state).vibey$avatar());
    }
    //?} else if >=1.21.2 {
    /*@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/PlayerRenderState;)V", at = @At("TAIL"))
    private void vibey$emote(net.minecraft.client.renderer.entity.state.PlayerRenderState state, CallbackInfo ci) {
        HumanoidModel<?> m = (HumanoidModel<?>) (Object) this;
        AvatarHooks.emotePose(m.head, m.body, m.rightArm, m.leftArm, m.rightLeg, m.leftLeg,
                ((dev.henny.vibeyclient.avatar.render.AvatarCarrier) state).vibey$avatar());
    }
    *///?} else {
    /*@Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void vibey$emote(net.minecraft.world.entity.LivingEntity entity, float a, float b, float c, float d, float e, CallbackInfo ci) {
        if (!(entity instanceof net.minecraft.world.entity.player.Player p)) return;
        PlayerModel<?> m = (PlayerModel<?>) (Object) this;
        var st = dev.henny.vibeyclient.avatar.AvatarStates.peek(p.getUUID());
        if (st == null || st.emote == null) return;
        AvatarHooks.emotePose(m.head, m.body, m.rightArm, m.leftArm, m.rightLeg, m.leftLeg, st);
        m.hat.copyFrom(m.head);
        m.jacket.copyFrom(m.body);
        m.leftSleeve.copyFrom(m.leftArm);
        m.rightSleeve.copyFrom(m.rightArm);
        m.leftPants.copyFrom(m.leftLeg);
        m.rightPants.copyFrom(m.rightLeg);
    }
    *///?}
}
