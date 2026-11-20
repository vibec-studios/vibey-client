package dev.henny.vibeyclient.module.cinematic.mixin;

import dev.henny.vibeyclient.module.cinematic.run.AfkHud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
abstract class CinematicGuiMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void vibey$afkHud(GuiGraphics g, DeltaTracker tracker, CallbackInfo ci) {
        if (AfkHud.render(g)) ci.cancel();
    }
}
