package dev.henny.vibeyclient.module.pvp.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.vibeyclient.module.pvp.PvpHooks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Low fire: moves the first-person fire overlay down (the burning itself is unchanged). */
@Mixin(ScreenEffectRenderer.class)
abstract class ScreenEffectRendererMixin {
    @Unique
    private static boolean vibey$lowered;

    @Inject(method = "renderFire", at = @At("HEAD"))
    private static void vibey$lowFire(PoseStack poseStack, MultiBufferSource buffers, TextureAtlasSprite sprite, CallbackInfo ci) {
        float down = PvpHooks.fireOffset();
        vibey$lowered = down > 0;
        if (!vibey$lowered) return;
        poseStack.pushPose();
        poseStack.translate(0f, -down, 0f);
    }

    @Inject(method = "renderFire", at = @At("RETURN"))
    private static void vibey$lowFireEnd(PoseStack poseStack, MultiBufferSource buffers, TextureAtlasSprite sprite, CallbackInfo ci) {
        if (!vibey$lowered) return;
        vibey$lowered = false;
        poseStack.popPose();
    }
}
