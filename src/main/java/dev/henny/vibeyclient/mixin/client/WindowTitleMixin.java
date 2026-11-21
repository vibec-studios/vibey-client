package dev.henny.vibeyclient.mixin.client;

import dev.henny.vibeyclient.BuildInfo;
import dev.henny.vibeyclient.client.branding.GameVersion;
import dev.henny.vibeyclient.client.branding.WindowTitle;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class WindowTitleMixin {
    @Inject(method = "createTitle", at = @At("RETURN"), cancellable = true)
    private void vibeyclient$title(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(WindowTitle.compose(cir.getReturnValue(), BuildInfo.VERSION, GameVersion.name()));
    }
}
