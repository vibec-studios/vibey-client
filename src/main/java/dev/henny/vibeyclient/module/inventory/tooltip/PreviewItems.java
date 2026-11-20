package dev.henny.vibeyclient.module.inventory.tooltip;

import dev.henny.vibeyclient.module.inventory.InventoryModule;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import java.util.ArrayList;
import java.util.List;

public final class PreviewItems {
    public static final int COLUMNS = 9, SHULKER_SIZE = 27, MAX_SIZE = 54;
    private static final int PLAIN_TINT = 0xFF8B6CFF;

    private PreviewItems() {}

    public static boolean replacesList(ItemStack stack) {
        return of(stack) != null;
    }

    public static ContainerPreview of(ItemStack stack) {
        InventoryModule m = InventoryModule.get();
        if (m == null || !m.previewActive() || stack.isEmpty()) return null;
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents == null || !contents.nonEmptyItems().iterator().hasNext()) return null;
        List<ItemStack> items = new ArrayList<>(contents.stream().toList());
        int size = Math.min(MAX_SIZE, Math.max(shulker(stack) ? SHULKER_SIZE : 0, items.size()));
        while (items.size() > size) items.remove(items.size() - 1);
        while (items.size() < size || items.size() % COLUMNS != 0) items.add(ItemStack.EMPTY);
        return new ContainerPreview(List.copyOf(items), COLUMNS, tint(stack));
    }

    private static boolean shulker(ItemStack stack) {
        return stack.getItem() instanceof BlockItem b && b.getBlock() instanceof ShulkerBoxBlock;
    }

    private static int tint(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem b) || !(b.getBlock() instanceof ShulkerBoxBlock box)) return PLAIN_TINT;
        DyeColor color = box.getColor();
        return color == null ? PLAIN_TINT : 0xFF000000 | color.getTextureDiffuseColor();
    }
}
