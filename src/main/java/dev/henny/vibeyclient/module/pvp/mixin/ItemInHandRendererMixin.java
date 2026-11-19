package dev.henny.vibeyclient.module.pvp.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.henny.vibeyclient.module.pvp.PvpHooks;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lower shield: moves a held shield down in first person, idle and while blocking (only how it looks: blocking works
 * exactly like vanilla). The pose stack is pushed/popped by vanilla around this, so nothing needs undoing.
 */
@Mixin(ItemInHandRenderer.class)
abstract class ItemInHandRendererMixin {
    @Inject(method = "renderArmWithItem", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"))
    private void vibey$lowShield(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand, float swing,
                                 ItemStack stack, float equip, PoseStack poseStack, SubmitNodeCollector collector, int light,
                                 CallbackInfo ci) {
        if (!(stack.getItem() instanceof ShieldItem)) return;
        float down = PvpHooks.shieldOffset();
        if (down > 0) poseStack.translate(0f, -down, 0f);
    }
}
