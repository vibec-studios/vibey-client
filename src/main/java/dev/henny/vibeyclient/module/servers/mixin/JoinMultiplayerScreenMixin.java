package dev.henny.vibeyclient.module.servers.mixin;

import dev.henny.vibeyclient.module.servers.ping.PingStore;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
abstract class JoinMultiplayerScreenMixin {
    @Inject(method = "refreshServerList", at = @At("HEAD"))
    private void vibey$forgetPings(CallbackInfo ci) {
        PingStore.clear();
    }
}
