package dev.henny.vibeyclient.module.screens.mixin;

import dev.henny.vibeyclient.module.screens.apply.Sessions;
import dev.henny.vibeyclient.module.screens.hook.ScreenFrame;
import dev.henny.vibeyclient.module.screens.hook.ScreenStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
abstract class ScreenLifecycleMixin {
    @ModifyVariable(method = "init(II)V", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private int vibey$virtualWidth(int w) {
        Sessions.beforeReposition((Screen) (Object) this);
        return ScreenFrame.virtual((Screen) (Object) this, w, true);
    }

    @ModifyVariable(method = "init(II)V", at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0)
    private int vibey$virtualHeight(int h) {
        return ScreenFrame.virtual((Screen) (Object) this, h, false);
    }

    @ModifyVariable(method = "resize", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private int vibey$resizeWidth(int w) {
        Sessions.beforeReposition((Screen) (Object) this);
        return ScreenFrame.virtual((Screen) (Object) this, w, true);
    }

    @ModifyVariable(method = "resize", at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0)
    private int vibey$resizeHeight(int h) {
        return ScreenFrame.virtual((Screen) (Object) this, h, false);
    }

    @Inject(method = "init(II)V", at = @At("TAIL"), require = 0)
    private void vibey$afterInit(int w, int h, CallbackInfo ci) {
        Sessions.afterInit((Screen) (Object) this);
    }

    @Inject(method = "resize", at = @At("TAIL"), require = 0)
    private void vibey$afterResize(int w, int h, CallbackInfo ci) {
        Sessions.afterInit((Screen) (Object) this);
    }

    @Inject(method = "rebuildWidgets", at = @At("TAIL"), require = 0)
    private void vibey$afterRebuild(CallbackInfo ci) {
        Sessions.afterInit((Screen) (Object) this);
    }

    @Inject(method = "renderTransparentBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void vibey$dim(GuiGraphics g, CallbackInfo ci) {
        if (ScreenStyle.transparentBackground((Screen) (Object) this, g)) ci.cancel();
    }

    @Inject(method = "renderMenuBackground(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void vibey$menuDim(GuiGraphics g, CallbackInfo ci) {
        if (ScreenStyle.menuBackground((Screen) (Object) this, g)) ci.cancel();
    }
}
