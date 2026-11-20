package dev.henny.vibeyclient.module.inventory.presets;

import dev.henny.vibeyclient.module.inventory.click.Intent;
import dev.henny.vibeyclient.module.inventory.core.Click;
import dev.henny.vibeyclient.module.inventory.core.HotbarPreset;
import dev.henny.vibeyclient.module.inventory.core.PresetPlanner;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.List;

final class PresetIntent implements Intent {
    static final int MAX_SWAPS = HotbarPreset.SIZE;

    private final HotbarPreset preset;
    private int swaps;

    PresetIntent(HotbarPreset preset) {
        this.preset = preset;
    }

    @Override
    public int containerId() {
        return 0;
    }

    @Override
    public boolean valid(AbstractContainerMenu menu) {
        return menu instanceof InventoryMenu && Intent.super.valid(menu);
    }

    @Override
    public List<Click> next(AbstractContainerMenu menu) {
        var player = Minecraft.getInstance().player;
        if (player == null || swaps >= MAX_SWAPS) return List.of();
        PresetPlanner.Swap swap = PresetPlanner.next(ItemRefs.inventory(player.getInventory()), preset);
        if (swap == null) return List.of();
        swaps++;
        int slot = swap.from() < HotbarPreset.SIZE ? InventoryMenu.USE_ROW_SLOT_START + swap.from() : swap.from();
        return List.of(Click.swap(slot, swap.hotbar()));
    }
}
