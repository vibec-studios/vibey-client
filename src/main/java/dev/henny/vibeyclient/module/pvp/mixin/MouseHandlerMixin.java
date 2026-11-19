package dev.henny.vibeyclient.module.pvp.mixin;

import dev.henny.vibeyclient.module.pvp.Zoom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Zoom input: slower mouse in proportion to the zoom (the options are never changed), optional cinematic smoothing
 * while zoomed, and the mouse wheel steps the zoom level instead of the hotbar while the zoom key is held.
 */
@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
    @Shadow @Final private Minecraft minecraft;

    @ModifyArg(method = "turnPlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"), index = 0)
    private double vibey$zoomYaw(double dx) {
        return dx * Zoom.mouseScale();
    }

    @ModifyArg(method = "turnPlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"), index = 1)
    private double vibey$zoomPitch(double dy) {
        return dy * Zoom.mouseScale();
    }

    @Redirect(method = "turnPlayer", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/Options;smoothCamera:Z", opcode = org.objectweb.asm.Opcodes.GETFIELD))
    private boolean vibey$cinematic(Options options) {
        return options.smoothCamera || Zoom.cinematic();
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void vibey$zoomScroll(long window, double dx, double dy, CallbackInfo ci) {
        Minecraft mc = minecraft;
        if (window != mc.getWindow().handle() || mc.screen != null || mc.getOverlay() != null || mc.player == null) return;
        if (Zoom.scroll(dy)) ci.cancel();
    }
}
