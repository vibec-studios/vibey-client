package dev.henny.vibeyclient.module.visuals.mixin.items;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.vibeyclient.module.visuals.items.core.SwingStyle;
import dev.henny.vibeyclient.module.visuals.items.swing.SwingRender;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemInHandRenderer.class)
abstract class HeldSwingMixin {
    private static final String ARM_WITH_ITEM = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderArmWithItem("
            + "Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;F"
            + "Lnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V";
    private static final String SWING_ARM = "Lnet/minecraft/client/renderer/ItemInHandRenderer;swingArm("
            + "FLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V";

    @WrapOperation(method = "renderArmWithItem", require = 0, at = @At(value = "INVOKE", target = SWING_ARM))
    private void vibey$swing(ItemInHandRenderer self, float progress, PoseStack stack, int side, HumanoidArm arm,
                             Operation<Void> original) {
        SwingStyle style = SwingRender.style();
        if (style.ownCurve()) SwingRender.apply(stack, style, progress, side);
        else original.call(self, progress, stack, side, arm);
    }

    @ModifyArg(method = "renderHandsWithItems", require = 0, index = 6, at = @At(value = "INVOKE", target = ARM_WITH_ITEM))
    private float vibey$equip(float progress) {
        return SwingRender.equip(progress);
    }

    @Inject(method = "shouldInstantlyReplaceVisibleItem", require = 0, cancellable = true, at = @At("HEAD"))
    private void vibey$instant(ItemStack shown, ItemStack held, CallbackInfoReturnable<Boolean> cir) {
        if (SwingRender.instantSwap()) cir.setReturnValue(true);
    }

    @WrapOperation(method = "tick", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;getItemSwapScale(F)F"))
    private float vibey$swap(LocalPlayer player, float partial, Operation<Float> original) {
        return SwingRender.swapScale(original.call(player, partial));
    }

    @Inject(method = "renderPlayerArm", require = 0, cancellable = true, at = @At("HEAD"))
    private void vibey$emptyHand(PoseStack stack, SubmitNodeCollector collector, int light, float equip, float swing,
                                 HumanoidArm arm, CallbackInfo ci) {
        if (SwingRender.hideEmptyHand()) ci.cancel();
    }
}
