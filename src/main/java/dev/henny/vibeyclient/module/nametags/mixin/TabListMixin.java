package dev.henny.vibeyclient.module.nametags.mixin;

import dev.henny.vibeyclient.client.names.Profiles;
import dev.henny.vibeyclient.module.nametags.NameIcons;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerTabOverlay.class)
public abstract class TabListMixin {
    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void vibey$icons(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
        Component name = cir.getReturnValue();
        Component out = NameIcons.tabList(Profiles.id(info.getProfile()), Profiles.name(info.getProfile()), name);
        if (out != name) cir.setReturnValue(out);
    }
}
