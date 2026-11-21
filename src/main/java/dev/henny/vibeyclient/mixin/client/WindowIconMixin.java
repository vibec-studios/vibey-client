package dev.henny.vibeyclient.mixin.client;

import com.mojang.blaze3d.platform.IconSet;
import dev.henny.vibeyclient.client.branding.WindowIcons;
import net.minecraft.server.packs.resources.IoSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;
import java.util.List;

@Mixin(IconSet.class)
public abstract class WindowIconMixin {
    @Inject(method = "getStandardIcons", at = @At("RETURN"), cancellable = true)
    private void vibeyclient$icons(CallbackInfoReturnable<List<IoSupplier<InputStream>>> cir) {
        List<IoSupplier<InputStream>> own = WindowIcons.standard();
        if (own != null) cir.setReturnValue(own);
    }
}
