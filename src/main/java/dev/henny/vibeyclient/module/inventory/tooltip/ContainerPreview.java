package dev.henny.vibeyclient.module.inventory.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record ContainerPreview(List<ItemStack> items, int columns, int tint) implements TooltipComponent {
    public int rows() {
        return (items.size() + columns - 1) / columns;
    }
}
