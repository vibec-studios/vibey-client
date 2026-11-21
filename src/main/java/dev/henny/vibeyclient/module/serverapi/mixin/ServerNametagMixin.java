package dev.henny.vibeyclient.module.serverapi.mixin;

import dev.henny.vibeyclient.client.ServerPresentationClient;
import dev.henny.vibeyclient.module.serverapi.ServerApiModule;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(EntityRenderer.class)
abstract class ServerNametagMixin {
    @Inject(method = "extractRenderState", at = @At("RETURN"), require = 1)
    private void vibey$serverNametag(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
        if (state.nameTag == null || !(entity instanceof Player) || !ServerApiModule.enabled()) return;
        Integer color = ServerPresentationClient.nametagColor(entity.getUUID());
        if (color == null) return;
        var tinted = Component.empty();
        state.nameTag.visit((style, text) -> {
            tinted.append(Component.literal(text).setStyle(style.withColor(color)));
            return Optional.empty();
        }, Style.EMPTY);
        state.nameTag = tinted;
    }
}
