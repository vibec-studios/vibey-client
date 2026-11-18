package dev.henny.vibeyclient.module.cosmetics.mixin;

import dev.henny.vibeyclient.avatar.render.SkinOverride;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//? if >=1.21.9 {
import net.minecraft.world.entity.player.PlayerSkin;
//?} else
/*import net.minecraft.client.resources.PlayerSkin;*/

/** Our cape texture on the vanilla cape model (and the elytra), keeping vanilla's cape physics. */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void vibey$cape(CallbackInfoReturnable<PlayerSkin> cir) {
        PlayerSkin out = SkinOverride.apply((AbstractClientPlayer) (Object) this, cir.getReturnValue());
        if (out != null) cir.setReturnValue(out);
    }
}
