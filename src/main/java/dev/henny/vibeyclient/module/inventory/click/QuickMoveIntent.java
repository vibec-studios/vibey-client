package dev.henny.vibeyclient.module.inventory.click;

import dev.henny.vibeyclient.module.inventory.core.Click;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.List;

public final class QuickMoveIntent implements Intent {
    private final int containerId;
    private final int slot;
    private boolean done;

    public QuickMoveIntent(int containerId, int slot) {
        this.containerId = containerId;
        this.slot = slot;
    }

    @Override
    public int containerId() {
        return containerId;
    }

    @Override
    public List<Click> next(AbstractContainerMenu menu) {
        if (done || slot < 0 || slot >= menu.slots.size()) return List.of();
        done = true;
        return menu.getSlot(slot).hasItem() ? List.of(Click.quickMove(slot)) : List.of();
    }
}
