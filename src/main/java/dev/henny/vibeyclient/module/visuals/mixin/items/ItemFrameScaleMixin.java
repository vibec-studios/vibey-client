package dev.henny.vibeyclient.module.visuals.mixin.items;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.vibeyclient.module.visuals.items.drop.DropRender;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemFrameRenderer.class)
abstract class ItemFrameScaleMixin {
    private static final float ITEM_SCALE = 0.5f;

    @WrapOperation(method = "submit", require = 0, at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"))
    private void vibey$scale(PoseStack stack, float x, float y, float z, Operation<Void> original) {
        float s = x == ITEM_SCALE && y == ITEM_SCALE ? DropRender.framed() : 1f;
        original.call(stack, x * s, y * s, z * s);
    }
}
