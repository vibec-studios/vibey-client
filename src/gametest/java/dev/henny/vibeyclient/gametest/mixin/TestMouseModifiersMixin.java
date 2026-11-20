package dev.henny.vibeyclient.gametest.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MouseHandler.class)
abstract class TestMouseModifiersMixin {
    private static final int SHIFT = 1, CONTROL = 2;

    @ModifyVariable(method = "onButton", at = @At("HEAD"), argsOnly = true)
    private MouseButtonInfo vibey$heldModifiers(MouseButtonInfo info) {
        Minecraft mc = Minecraft.getInstance();
        int mods = info.modifiers() | (mc.hasShiftDown() ? SHIFT : 0) | (mc.hasControlDown() ? CONTROL : 0);
        return mods == info.modifiers() ? info : new MouseButtonInfo(info.button(), mods);
    }
}
