package dev.henny.vibeyclient.module.visuals.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.vibeyclient.module.visuals.items.ItemPhysics;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
abstract class ItemEntityRendererMixin {
    private static final String EXTRACT = "extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;"
            + "Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V";
    private static final String SUBMIT = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;"
            + "Lnet/minecraft/client/renderer/state/CameraRenderState;)V";

    @Inject(method = EXTRACT, require = 0, at = @At("TAIL"))
    private void vibey$extract(ItemEntity entity, ItemEntityRenderState state, float partialTick, CallbackInfo ci) {
        ItemPhysics.extract(entity, state);
    }

    @WrapOperation(method = SUBMIT, require = 0, at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private void vibey$lift(PoseStack stack, float x, float y, float z, Operation<Void> original,
                            @Local(argsOnly = true) ItemEntityRenderState state) {
        if (ItemPhysics.active(state)) original.call(stack, 0f, ItemPhysics.liftY(state, y), 0f);
        else original.call(stack, x, ItemPhysics.liftY(state, y), z);
    }

    @WrapOperation(method = SUBMIT, require = 0, at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V"))
    private void vibey$rotate(PoseStack stack, Quaternionfc rotation, Operation<Void> original,
                              @Local(argsOnly = true) ItemEntityRenderState state) {
        if (ItemPhysics.active(state)) ItemPhysics.rotate(stack, state);
        else {
            original.call(stack, rotation);
            ItemPhysics.scaleVanilla(stack);
        }
    }
}
