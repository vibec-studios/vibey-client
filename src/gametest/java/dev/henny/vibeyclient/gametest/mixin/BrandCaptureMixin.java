package dev.henny.vibeyclient.gametest.mixin;

import dev.henny.vibeyclient.gametest.BrandingGameTest;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class BrandCaptureMixin {
    @Inject(method = "handleCustomPayload", at = @At("HEAD"))
    private void vibeyclient$captureBrand(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        if (packet.payload() instanceof BrandPayload brand) BrandingGameTest.serverSaw(brand.brand());
    }
}
