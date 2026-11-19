package dev.henny.vibeyclient.module.clips.mixin;

import dev.henny.vibeyclient.module.clips.ClipsHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Kill signals for automatic clips: damage events (who hit whom) and entity event 3 (death). TAIL: the handlers first
 * hop to the client thread, only that second call reaches the end.
 */
@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerMixin {
    @Inject(method = "handleDamageEvent", at = @At("TAIL"))
    private void vibey$clipsDamage(ClientboundDamageEventPacket packet, CallbackInfo ci) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        ClipsHooks.damage(level.getEntity(packet.entityId()), packet.sourceCauseId(), packet.sourceDirectId());
    }

    @Inject(method = "handleEntityEvent", at = @At("TAIL"))
    private void vibey$clipsEntityEvent(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        var level = Minecraft.getInstance().level;
        if (level == null || packet.getEventId() != 3) return;
        ClipsHooks.entityEvent(packet.getEntity(level), packet.getEventId());
    }
}
