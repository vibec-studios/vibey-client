package dev.henny.vibeyclient.module.screens.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.henny.vibeyclient.module.screens.hook.ScreenStyle;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Options.class)
abstract class OptionsBlurMixin {
    @ModifyReturnValue(method = "getMenuBackgroundBlurriness", at = @At("RETURN"), require = 0)
    private int vibey$screenBlur(int vanilla) {
        return ScreenStyle.blur(vanilla);
    }
}
