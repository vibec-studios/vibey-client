package dev.henny.cosmetico.mc.mixin;

import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.mc.player.ShieldDraw;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=26.3 {
/*@Mixin(net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer.class)
*///?} else
@Mixin(net.minecraft.client.renderer.ItemInHandRenderer.class)
public abstract class FirstPersonItemMixin {
    //? if >=26.2 {
    /*@Inject(method = "submitArmWithItem", at = @At("HEAD"))
    *///?} else
    @Inject(method = "renderArmWithItem", at = @At("HEAD"))
    private void cosmetico$owner(CallbackInfo ci) {
        ShieldDraw.owner = Cosmetico.config().self == null ? null : Cosmetico.engine().avatars.peek(Cosmetico.config().self);
    }

    //? if >=26.2 {
    /*@Inject(method = "submitArmWithItem", at = @At("RETURN"))
    *///?} else
    @Inject(method = "renderArmWithItem", at = @At("RETURN"))
    private void cosmetico$ownerEnd(CallbackInfo ci) {
        ShieldDraw.owner = null;
    }
}
