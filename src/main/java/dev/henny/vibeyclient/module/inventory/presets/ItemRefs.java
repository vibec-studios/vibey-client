package dev.henny.vibeyclient.module.inventory.presets;

import dev.henny.vibeyclient.module.inventory.core.HotbarPreset;
import dev.henny.vibeyclient.module.inventory.core.ItemRef;
import dev.henny.vibeyclient.module.inventory.core.PresetPlanner;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class ItemRefs {
    private ItemRefs() {}

    public static ItemRef of(ItemStack st) {
        if (st == null || st.isEmpty()) return ItemRef.EMPTY;
        Component custom = st.get(DataComponents.CUSTOM_NAME);
        return new ItemRef(BuiltInRegistries.ITEM.getKey(st.getItem()).toString(), custom == null ? null : custom.getString());
    }

    public static List<ItemRef> inventory(Inventory inv) {
        List<ItemRef> out = new ArrayList<>(PresetPlanner.INVENTORY);
        for (int i = 0; i < PresetPlanner.INVENTORY; i++) out.add(of(inv.getItem(i)));
        return out;
    }

    public static HotbarPreset hotbar(Inventory inv) {
        List<ItemRef> out = new ArrayList<>(HotbarPreset.SIZE);
        for (int i = 0; i < HotbarPreset.SIZE; i++) out.add(of(inv.getItem(i)));
        return new HotbarPreset(out);
    }

    public static ItemStack icon(ItemRef r) {
        if (r == null || r.empty()) return ItemStack.EMPTY;
        Identifier id = Identifier.tryParse(r.id());
        if (id == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.getValue(id);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }
}
