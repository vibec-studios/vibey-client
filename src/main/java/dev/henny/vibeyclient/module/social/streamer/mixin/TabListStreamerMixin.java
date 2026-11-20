package dev.henny.vibeyclient.module.social.streamer.mixin;

import dev.henny.vibeyclient.client.names.Profiles;
import dev.henny.vibeyclient.module.social.streamer.Aliases;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerTabOverlay.class, priority = 900)
public abstract class TabListStreamerMixin {
    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void vibey$streamerName(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
        Component alias = Aliases.of(Profiles.name(info.getProfile()));
        if (alias != null) cir.setReturnValue(alias);
    }
}
