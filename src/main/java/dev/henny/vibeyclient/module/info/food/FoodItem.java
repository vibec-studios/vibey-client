package dev.henny.vibeyclient.module.info.food;

import dev.henny.vibeyclient.module.info.core.FoodTable;
import dev.henny.vibeyclient.module.info.core.FoodValue;
import dev.henny.vibeyclient.module.info.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class FoodItem {
    private FoodItem() {}

    public static ItemStack pick(Minecraft mc) {
        if (mc.screen instanceof AbstractContainerScreenAccessor screen) {
            Slot slot = screen.vibey$hoveredSlot();
            return slot != null && slot.hasItem() ? slot.getItem() : ItemStack.EMPTY;
        }
        if (mc.screen != null) return ItemStack.EMPTY;
        ItemStack main = mc.player.getMainHandItem();
        return value(main) != null ? main : mc.player.getOffhandItem();
    }

    public static FoodValue value(ItemStack stack) {
        if (stack.isEmpty()) return null;
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food != null) return new FoodValue(food.nutrition(), food.saturation());
        return FoodTable.withoutComponent(id(stack));
    }

    public static String id(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
