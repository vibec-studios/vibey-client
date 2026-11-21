package dev.henny.vibeyclient.module.pvp.mixin;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
//? if <1.21.2 {
/*import dev.henny.vibeyclient.module.pvp.ToggleMovement;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*///?}

@Mixin(KeyMapping.class)
abstract class KeyMappingToggleMixin {
    //? if <1.21.2 {
    /*@Inject(method = "isDown", require = 0, at = @At("RETURN"), cancellable = true)
    private void vibey$toggles(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && ToggleMovement.held((KeyMapping) (Object) this)) cir.setReturnValue(true);
    }
    *///?}
}
