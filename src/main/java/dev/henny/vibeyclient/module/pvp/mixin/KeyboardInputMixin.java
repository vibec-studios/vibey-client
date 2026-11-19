package dev.henny.vibeyclient.module.pvp.mixin;

import dev.henny.vibeyclient.module.pvp.ToggleMovement;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Toggle sprint / sneak: reports the vanilla sprint/sneak key as held while it is toggled on. */
@Mixin(KeyboardInput.class)
abstract class KeyboardInputMixin {
    @Inject(method = "tick", require = 0, at = @At("RETURN"))
    private void vibey$toggles(CallbackInfo ci) {
        boolean sprint = ToggleMovement.sprintHeld(), sneak = ToggleMovement.sneakHeld();
        if (!sprint && !sneak) return;
        ClientInput self = (ClientInput) (Object) this;
        Input k = self.keyPresses;
        if ((!sprint || k.sprint()) && (!sneak || k.shift())) return;
        self.keyPresses = new Input(k.forward(), k.backward(), k.left(), k.right(), k.jump(), k.shift() || sneak, k.sprint() || sprint);
    }
}
