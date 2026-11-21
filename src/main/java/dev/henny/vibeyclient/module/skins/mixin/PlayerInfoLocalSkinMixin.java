package dev.henny.vibeyclient.module.skins.mixin;

import dev.henny.vibeyclient.module.skins.LocalSkin;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInfo.class)
public abstract class PlayerInfoLocalSkinMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void vibey$localSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        PlayerSkin out = LocalSkin.apply((PlayerInfo) (Object) this, cir.getReturnValue());
        if (out != null) cir.setReturnValue(out);
    }
}
