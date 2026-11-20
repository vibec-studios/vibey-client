package dev.henny.vibeyclient.module.visuals.mixin.items;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.vibeyclient.module.visuals.items.held.HeldRender;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
abstract class HeldItemMixin {
    @Inject(method = "applyItemArmTransform", require = 0, at = @At("TAIL"))
    private void vibey$offset(PoseStack stack, HumanoidArm arm, float equip, CallbackInfo ci) {
        HeldRender.translate(stack, arm);
    }

    @Inject(method = "renderItem", require = 0, at = @At("HEAD"))
    private void vibey$pose(LivingEntity entity, ItemStack stack, ItemDisplayContext context, PoseStack pose,
                            SubmitNodeCollector collector, int light, CallbackInfo ci) {
        HeldRender.transform(pose, entity, context);
    }
}
