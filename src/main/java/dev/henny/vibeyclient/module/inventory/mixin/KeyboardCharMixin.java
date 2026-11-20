package dev.henny.vibeyclient.module.inventory.mixin;

import dev.henny.vibeyclient.module.inventory.screen.ContainerSessions;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
abstract class KeyboardCharMixin {
    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void vibey$searchTyped(long window, CharacterEvent event, CallbackInfo ci) {
        if (ContainerSessions.typed(event.codepoint())) ci.cancel();
    }
}
