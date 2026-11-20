package dev.henny.vibeyclient.module.inventory.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.henny.vibeyclient.module.inventory.tooltip.ContainerPreview;
import dev.henny.vibeyclient.module.inventory.tooltip.PreviewItems;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(ItemStack.class)
abstract class ItemStackTooltipMixin {
    @ModifyReturnValue(method = "getTooltipImage", at = @At("RETURN"))
    private Optional<TooltipComponent> vibey$containerPreview(Optional<TooltipComponent> original) {
        if (original.isPresent()) return original;
        ContainerPreview preview = PreviewItems.of((ItemStack) (Object) this);
        return preview == null ? original : Optional.of(preview);
    }

    @Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
    private void vibey$hideContainerList(DataComponentType<?> type, Item.TooltipContext context, TooltipDisplay display,
                                         Consumer<Component> out, TooltipFlag flag, CallbackInfo ci) {
        if (type == DataComponents.CONTAINER && PreviewItems.replacesList((ItemStack) (Object) this)) ci.cancel();
    }
}
