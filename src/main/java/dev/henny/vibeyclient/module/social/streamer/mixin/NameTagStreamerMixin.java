package dev.henny.vibeyclient.module.social.streamer.mixin;

import dev.henny.vibeyclient.client.names.Profiles;
import dev.henny.vibeyclient.module.social.streamer.Aliases;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityRenderer.class, priority = 900)
public abstract class NameTagStreamerMixin {
    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void vibey$streamerName(Entity entity, CallbackInfoReturnable<Component> cir) {
        if (!(entity instanceof Player player) || cir.getReturnValue() == null) return;
        Component alias = Aliases.of(Profiles.name(player.getGameProfile()));
        if (alias != null) cir.setReturnValue(alias);
    }
}
