package dev.henny.vibeyclient.module.accessibility.mixin;

import dev.henny.vibeyclient.module.accessibility.AccessibilityModule;
import net.minecraft.client.gui.components.SubtitleOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SubtitleOverlay.class)
abstract class SubtitleOverlayMixin {
    //? if >=26.1 {
    /*@Unique private static final String METHOD = "extractRenderState";
    @Unique private static final String GRAPHICS = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;";
    @Unique private static final String TEXT = "text";
    *///?} else {
    @Unique private static final String METHOD = "render";
    @Unique private static final String GRAPHICS = "Lnet/minecraft/client/gui/GuiGraphics;";
    @Unique private static final String TEXT = "drawString";
    //?}

    @ModifyArg(method = METHOD, at = @At(value = "INVOKE", target = GRAPHICS + "fill(IIIII)V"), index = 4, require = 1)
    private int vibey$subtitleBackground(int original) {
        return AccessibilityModule.subtitleStyle().background(original);
    }

    @ModifyArg(method = METHOD, at = @At(value = "INVOKE",
            target = GRAPHICS + TEXT + "(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"), index = 4, require = 2)
    private int vibey$subtitleArrow(int original) {
        return AccessibilityModule.subtitleStyle().text(original);
    }

    @ModifyArg(method = METHOD, at = @At(value = "INVOKE",
            target = GRAPHICS + TEXT + "(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"),
            index = 4, require = 1)
    private int vibey$subtitleText(int original) {
        return AccessibilityModule.subtitleStyle().text(original);
    }
}
