package dev.henny.vibeyclient.module.inventory.click;

import dev.henny.vibeyclient.module.inventory.core.Section;
import dev.henny.vibeyclient.module.inventory.core.SlotView;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class MenuSlots {
    private MenuSlots() {}

    public static List<SlotView> read(AbstractContainerMenu menu, ItemStack reference) {
        List<SlotView> out = new ArrayList<>(menu.slots.size());
        for (Slot s : menu.slots) out.add(view(menu, s, reference));
        return out;
    }

    public static SlotView view(AbstractContainerMenu menu, Slot s, ItemStack reference) {
        ItemStack st = s.getItem();
        Section section = section(menu, s, reference);
        boolean accepts = section != Section.NONE && (reference.isEmpty() || s.mayPlace(reference));
        int max = reference.isEmpty() ? st.getMaxStackSize() : Math.min(s.getMaxStackSize(reference), reference.getMaxStackSize());
        return new SlotView(s.index, section, key(st), st.getCount(), max, accepts);
    }

    public static boolean hasContainer(AbstractContainerMenu menu) {
        return !(menu instanceof InventoryMenu);
    }

    public static String key(ItemStack st) {
        if (st == null || st.isEmpty()) return SlotView.EMPTY;
        return BuiltInRegistries.ITEM.getKey(st.getItem()) + "#" + st.getComponentsPatch().hashCode();
    }

    private static Section section(AbstractContainerMenu menu, Slot s, ItemStack reference) {
        if (!s.isActive()) return Section.NONE;
        if (s.container instanceof Inventory) {
            int i = s.getContainerSlot();
            if (i >= 0 && i < 9) return Section.HOTBAR;
            if (i >= 9 && i < 36) return Section.MAIN;
            return Section.NONE;
        }
        if (menu instanceof InventoryMenu) return Section.NONE;
        ItemStack own = s.getItem();
        ItemStack probe = own.isEmpty() ? reference : own;
        return probe.isEmpty() || s.mayPlace(probe) ? Section.CONTAINER : Section.NONE;
    }
}
