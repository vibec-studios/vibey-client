package dev.henny.vibeyclient.module.inventory.mixin;

import dev.henny.vibeyclient.module.inventory.screen.ContainerSessions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
abstract class ContainerScreenSlotsMixin {
    @Inject(method = "renderSlots", at = @At("TAIL"))
    private void vibey$searchHighlight(GuiGraphics g, int mouseX, int mouseY, CallbackInfo ci) {
        ContainerSessions.afterSlots((AbstractContainerScreen<?>) (Object) this, g);
    }
}
