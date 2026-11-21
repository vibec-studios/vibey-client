package dev.henny.vibeyclient.module.pvp.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.client.GameCompat;
import dev.henny.vibeyclient.module.pvp.Zoom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Zoom input: slower mouse in proportion to the zoom (the options are never changed), optional cinematic smoothing
 * while zoomed, and the mouse wheel steps the zoom level instead of the hotbar while the zoom key is held.
 */
@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
    @Shadow @Final private Minecraft minecraft;

    /** Slower mouse while zoomed (one wrap for both axes, stacks with other mods that wrap or redirect the call). */
    @WrapOperation(method = "turnPlayer", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void vibey$zoomTurn(LocalPlayer player, double dx, double dy, Operation<Void> original) {
        double s = Zoom.mouseScale();
        original.call(player, dx * s, dy * s);
    }

    /** Cinematic smoothing while zoomed: vanilla's {@code smoothCamera} read, or-ed with ours. */
    @ModifyExpressionValue(method = "turnPlayer", require = 0, at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/Options;smoothCamera:Z", opcode = org.objectweb.asm.Opcodes.GETFIELD))
    private boolean vibey$cinematic(boolean smoothCamera) {
        return smoothCamera || Zoom.cinematic();
    }

    @Inject(method = "onScroll", require = 0, at = @At("HEAD"), cancellable = true)
    private void vibey$zoomScroll(long window, double dx, double dy, CallbackInfo ci) {
        Minecraft mc = minecraft;
        if (window != GameCompat.windowHandle() || ClientCompat.screen() != null || ClientCompat.loadingOverlay() || mc.player == null) return;
        if (Zoom.scroll(dy)) ci.cancel();
    }
}
