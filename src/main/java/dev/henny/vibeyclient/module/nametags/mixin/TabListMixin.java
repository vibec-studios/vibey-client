package dev.henny.vibeyclient.module.nametags.mixin;

import dev.henny.vibeyclient.module.nametags.NameIcons;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prepends the Vibey icons to the tab list name (same method on 1.21.1 – 26.3; the column width follows). */
@Mixin(PlayerTabOverlay.class)
public abstract class TabListMixin {
    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void vibey$icons(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
        //? if >=1.21.9 {
        java.util.UUID id = info.getProfile().id();
        //?} else
        /*java.util.UUID id = info.getProfile().getId();*/
        Component name = cir.getReturnValue();
        Component out = NameIcons.tabList(id, name);
        if (out != name) cir.setReturnValue(out);
    }
}
