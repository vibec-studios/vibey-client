package dev.henny.vibeyclient.module.pvp.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.vibeyclient.module.pvp.PvpHooks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Low fire: moves the first-person fire overlay down (the burning itself is unchanged). One {@code @WrapMethod}
 * around the whole method, so the push and pop always pair up, even when another mod cancels or changes
 * {@code renderFire}.
 */
@Mixin(ScreenEffectRenderer.class)
abstract class ScreenEffectRendererMixin {
    @WrapMethod(method = "renderFire", require = 0)
    private static void vibey$lowFire(PoseStack poseStack, MultiBufferSource buffers, TextureAtlasSprite sprite,
                                      Operation<Void> original) {
        float down = PvpHooks.fireOffset();
        if (down <= 0) {
            original.call(poseStack, buffers, sprite);
            return;
        }
        poseStack.pushPose();
        try {
            poseStack.translate(0f, -down, 0f);
            original.call(poseStack, buffers, sprite);
        } finally {
            poseStack.popPose();
        }
    }
}
