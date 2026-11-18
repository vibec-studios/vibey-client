package dev.henny.vibeyclient.module.cosmetics.mixin;

import dev.henny.vibeyclient.avatar.AvatarConfig;
import dev.henny.vibeyclient.avatar.AvatarStates;
import dev.henny.vibeyclient.avatar.render.ShieldDraw;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * First person: the shield cosmetic of the local player. 26.3 moved first-person hands into
 * FirstPersonHandsAndItemsRenderer (not ported yet; the mixin is empty there).
 */
//? if >=26.3 {
/*@Mixin(net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer.class)
public abstract class FirstPersonItemMixin {
}
*///?} else {
@Mixin(net.minecraft.client.renderer.ItemInHandRenderer.class)
public abstract class FirstPersonItemMixin {
    @Inject(method = "renderArmWithItem", at = @At("HEAD"))
    private void vibey$owner(CallbackInfo ci) {
        ShieldDraw.owner = AvatarConfig.self == null ? null : AvatarStates.peek(AvatarConfig.self);
    }

    @Inject(method = "renderArmWithItem", at = @At("RETURN"))
    private void vibey$ownerEnd(CallbackInfo ci) {
        ShieldDraw.owner = null;
    }
}
//?}
