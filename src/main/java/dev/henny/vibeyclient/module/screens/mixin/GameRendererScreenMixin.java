package dev.henny.vibeyclient.module.screens.mixin;

import dev.henny.vibeyclient.module.screens.hook.ScreenFrame;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class GameRendererScreenMixin {
    private static final String RENDER_SCREEN =
            "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphics;IIF)V";

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = RENDER_SCREEN), index = 0, require = 0)
    private GuiGraphics vibey$scaleScreen(GuiGraphics g) {
        return ScreenFrame.begin(g);
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = RENDER_SCREEN), index = 1, require = 0)
    private int vibey$mouseX(int x) {
        return ScreenFrame.mouseX(x);
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = RENDER_SCREEN), index = 2, require = 0)
    private int vibey$mouseY(int y) {
        return ScreenFrame.mouseY(y);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = RENDER_SCREEN, shift = At.Shift.AFTER), require = 0)
    private void vibey$unscaleScreen(CallbackInfo ci) {
        ScreenFrame.end();
    }
}
