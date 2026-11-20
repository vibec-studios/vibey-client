package dev.henny.vibeyclient.module.social.streamer.mixin;

import dev.henny.vibeyclient.api.privacy.Privacy;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInfo.class)
public abstract class PlayerInfoSkinMixin {
    @Inject(method = "getSkin", at = @At("HEAD"), cancellable = true)
    private void vibey$streamerSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        if (Privacy.hideSkins()) cir.setReturnValue(DefaultPlayerSkin.getDefaultSkin());
    }
}
