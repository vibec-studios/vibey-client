package dev.henny.vibeyclient.module.nametags.mixin;

import dev.henny.vibeyclient.client.names.Profiles;
import dev.henny.vibeyclient.module.nametags.NameIcons;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if >=1.21.2 {
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//?} else {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
*///?}

//? if >=1.21.2 {
@Mixin(EntityRenderer.class)
//?} else
/*@Mixin(PlayerRenderer.class)*/
public abstract class NameTagMixin {
    //? if >=1.21.2 {
    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void vibey$icons(Entity entity, CallbackInfoReturnable<Component> cir) {
        if (!(entity instanceof Player player)) return;
        Component name = cir.getReturnValue();
        Component out = NameIcons.nametag(player.getUUID(), Profiles.name(player.getGameProfile()), name);
        if (out != name) cir.setReturnValue(out);
    }
    //?} else {
    /*@ModifyVariable(method = "renderNameTag(Lnet/minecraft/client/player/AbstractClientPlayer;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IF)V",
            at = @At("HEAD"), argsOnly = true)
    private Component vibey$icons(Component name, AbstractClientPlayer player, Component same, PoseStack ps, MultiBufferSource buffers, int light, float partial) {
        return NameIcons.nametag(player.getUUID(), Profiles.name(player.getGameProfile()), name);
    }
    *///?}
}
