package dev.henny.vibeyclient.module.clips.mixin;

import dev.henny.vibeyclient.module.clips.ClipsHooks;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Our own melee hits, a backup "we hit P" signal for automatic clips. */
@Mixin(MultiPlayerGameMode.class)
abstract class MultiPlayerGameModeMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void vibey$clipsAttack(Player player, Entity target, CallbackInfo ci) {
        ClipsHooks.attack(target);
    }
}
