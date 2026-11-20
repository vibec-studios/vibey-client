package dev.henny.vibeyclient.module.info.card;

import dev.henny.vibeyclient.module.info.core.HarvestInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public record CardData(BlockPos pos, Entity entity, ItemStack icon, String title, String mod, HarvestInfo harvest,
                       int growth, String health) {
    public static final int NO_GROWTH = -1;

    public boolean hasGrowth() {
        return growth != NO_GROWTH;
    }

    public boolean mature() {
        return growth >= 100;
    }

    public boolean hasHarvest() {
        return harvest != null && (harvest.showTool() || !harvest.canHarvest());
    }

    public boolean hasStatus() {
        return hasHarvest() || hasGrowth() || health != null;
    }
}
