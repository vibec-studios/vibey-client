package dev.henny.vibeyclient.module.inventory.click;

import dev.henny.vibeyclient.module.inventory.core.Click;
import dev.henny.vibeyclient.module.inventory.core.MovePlanner;
import dev.henny.vibeyclient.module.inventory.core.SlotView;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class WheelIntent implements Intent {
    private final int containerId;
    private final int slot;
    private final boolean push;
    private final boolean whole;
    private boolean done;

    public WheelIntent(int containerId, int slot, boolean push, boolean whole) {
        this.containerId = containerId;
        this.slot = slot;
        this.push = push;
        this.whole = whole;
    }

    @Override
    public int containerId() {
        return containerId;
    }

    @Override
    public List<Click> next(AbstractContainerMenu menu) {
        if (done || slot < 0 || slot >= menu.slots.size()) return List.of();
        done = true;
        ItemStack reference = menu.getSlot(slot).getItem();
        if (reference.isEmpty()) return List.of();
        List<SlotView> views = MenuSlots.read(menu, reference);
        boolean box = MenuSlots.hasContainer(menu);
        if (push) return whole ? MovePlanner.pushStack(views, slot) : MovePlanner.pushOne(views, slot, box);
        return whole ? MovePlanner.pullStack(views, slot, box) : MovePlanner.pullOne(views, slot, box);
    }
}
