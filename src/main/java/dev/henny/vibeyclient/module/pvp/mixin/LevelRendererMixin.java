package dev.henny.vibeyclient.module.pvp.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.henny.vibeyclient.module.pvp.PvpHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Custom block outline: colour, thickness and an optional translucent fill instead of vanilla's thin black lines
 * (vanilla's high-contrast underlay is kept when that accessibility option is on).
 */
@Mixin(LevelRenderer.class)
abstract class LevelRendererMixin {
    @Shadow
    private void renderHitOutline(PoseStack poseStack, VertexConsumer consumer, double camX, double camY, double camZ,
                                  BlockOutlineRenderState state, int color, float lineWidth) {
        throw new AssertionError();
    }

    @Inject(method = "renderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void vibey$outline(MultiBufferSource.BufferSource buffers, PoseStack poseStack, boolean translucentPass,
                               LevelRenderState level, CallbackInfo ci) {
        if (!PvpHooks.outlineActive()) return;
        ci.cancel();
        BlockOutlineRenderState state = level.blockOutlineRenderState;
        if (state == null || state.isTranslucent() != translucentPass) return;
        Vec3 cam = level.cameraRenderState.pos;
        float base = Minecraft.getInstance().getWindow().getAppropriateLineWidth() / 2.5f;
        if (PvpHooks.outlineFill()) {
            PvpHooks.fillOutline(poseStack, buffers.getBuffer(RenderTypes.debugFilledBox()), state, cam);
            buffers.endBatch(RenderTypes.debugFilledBox());
        }
        if (state.highContrast()) {
            renderHitOutline(poseStack, buffers.getBuffer(RenderTypes.secondaryBlockOutline()), cam.x, cam.y, cam.z, state,
                    0xFF000000, PvpHooks.outlineWidth() * base + 4.5f);
        }
        renderHitOutline(poseStack, buffers.getBuffer(RenderTypes.lines()), cam.x, cam.y, cam.z, state,
                PvpHooks.outlineColor(), PvpHooks.outlineWidth() * base);
        buffers.endLastBatch();
    }
}
