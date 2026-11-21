package dev.henny.vibeyclient.module.accessibility.mixin;

import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import dev.henny.vibeyclient.module.accessibility.AccessibilityModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class WorldFilterMixin {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private CrossFrameResourcePool resourcePool;

    @Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V"), require = 1)
    private void vibey$filterWorld(CallbackInfo ci) {
        AccessibilityModule.filterWorld(minecraft, resourcePool);
    }
}
