package dev.henny.vibeyclient.mixin.client;

import dev.henny.vibeyclient.ui.title.TitleHook;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Hands every screen the game opens to {@link TitleHook}, which swaps the vanilla title screen for Vibey's. */
//? if >=26.2 {
/*@Mixin(net.minecraft.client.gui.Gui.class)
*///?} else
@Mixin(net.minecraft.client.Minecraft.class)
public abstract class ScreenSwapMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen vibeyclient$swapTitle(Screen screen) {
        return TitleHook.replace(screen);
    }
}
