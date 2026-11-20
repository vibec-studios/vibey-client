package dev.henny.vibeyclient.module.screens.mixin;

import dev.henny.vibeyclient.module.screens.hook.ScreenFrame;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(MouseHandler.class)
abstract class MouseScreenMixin {
    private static final String CLICKED = "Lnet/minecraft/client/gui/screens/Screen;mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z";
    private static final String RELEASED = "Lnet/minecraft/client/gui/screens/Screen;mouseReleased(Lnet/minecraft/client/input/MouseButtonEvent;)Z";
    private static final String SCROLLED = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDDD)Z";
    private static final String MOVED = "Lnet/minecraft/client/gui/screens/Screen;mouseMoved(DD)V";
    private static final String DRAGGED = "Lnet/minecraft/client/gui/screens/Screen;mouseDragged(Lnet/minecraft/client/input/MouseButtonEvent;DD)Z";

    @ModifyArg(method = "onButton", at = @At(value = "INVOKE", target = CLICKED), index = 0, require = 0)
    private MouseButtonEvent vibey$click(MouseButtonEvent e) {
        return ScreenFrame.event(e);
    }

    @ModifyArg(method = "onButton", at = @At(value = "INVOKE", target = RELEASED), index = 0, require = 0)
    private MouseButtonEvent vibey$release(MouseButtonEvent e) {
        return ScreenFrame.event(e);
    }

    @ModifyArg(method = "onScroll", at = @At(value = "INVOKE", target = SCROLLED), index = 0, require = 0)
    private double vibey$scrollX(double x) {
        return ScreenFrame.pos(x);
    }

    @ModifyArg(method = "onScroll", at = @At(value = "INVOKE", target = SCROLLED), index = 1, require = 0)
    private double vibey$scrollY(double y) {
        return ScreenFrame.pos(y);
    }

    @ModifyArg(method = "handleAccumulatedMovement", at = @At(value = "INVOKE", target = MOVED), index = 0, require = 0)
    private double vibey$moveX(double x) {
        return ScreenFrame.pos(x);
    }

    @ModifyArg(method = "handleAccumulatedMovement", at = @At(value = "INVOKE", target = MOVED), index = 1, require = 0)
    private double vibey$moveY(double y) {
        return ScreenFrame.pos(y);
    }

    @ModifyArg(method = "handleAccumulatedMovement", at = @At(value = "INVOKE", target = DRAGGED), index = 0, require = 0)
    private MouseButtonEvent vibey$drag(MouseButtonEvent e) {
        return ScreenFrame.event(e);
    }

    @ModifyArg(method = "handleAccumulatedMovement", at = @At(value = "INVOKE", target = DRAGGED), index = 1, require = 0)
    private double vibey$dragX(double dx) {
        return ScreenFrame.pos(dx);
    }

    @ModifyArg(method = "handleAccumulatedMovement", at = @At(value = "INVOKE", target = DRAGGED), index = 2, require = 0)
    private double vibey$dragY(double dy) {
        return ScreenFrame.pos(dy);
    }
}
