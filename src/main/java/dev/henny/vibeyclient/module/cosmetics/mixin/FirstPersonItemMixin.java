package dev.henny.vibeyclient.module.cosmetics.mixin;

import dev.henny.vibeyclient.avatar.AvatarConfig;
import dev.henny.vibeyclient.avatar.AvatarStates;
import dev.henny.vibeyclient.avatar.render.ShieldDraw;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * First person: the shield cosmetic of the local player (owner for {@code ShieldDraw} while a hand is drawn).
 * 26.2 renamed renderArmWithItem to submitArmWithItem; 26.3 moved first-person hands into
 * FirstPersonHandsAndItemsRenderer (the item is still submitted inside submitArmWithItem).
 */
//? if >=26.3 {
/*@Mixin(net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer.class)
*///?} else
@Mixin(net.minecraft.client.renderer.ItemInHandRenderer.class)
public abstract class FirstPersonItemMixin {
    //? if >=26.2 {
    /*@Inject(method = "submitArmWithItem", at = @At("HEAD"))
    *///?} else
    @Inject(method = "renderArmWithItem", at = @At("HEAD"))
    private void vibey$owner(CallbackInfo ci) {
        ShieldDraw.owner = AvatarConfig.self == null ? null : AvatarStates.peek(AvatarConfig.self);
    }

    //? if >=26.2 {
    /*@Inject(method = "submitArmWithItem", at = @At("RETURN"))
    *///?} else
    @Inject(method = "renderArmWithItem", at = @At("RETURN"))
    private void vibey$ownerEnd(CallbackInfo ci) {
        ShieldDraw.owner = null;
    }
}
