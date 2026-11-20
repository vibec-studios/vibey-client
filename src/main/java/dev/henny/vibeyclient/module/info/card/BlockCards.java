package dev.henny.vibeyclient.module.info.card;

import dev.henny.vibeyclient.module.info.InfoModule;
import dev.henny.vibeyclient.module.info.core.HarvestInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockCards {
    private BlockCards() {}

    public static CardData read(Level level, BlockPos pos, BlockState state, Player player, InfoModule m) {
        ItemStack icon = state.getCloneItemStack(level, pos, false);
        if (icon.isEmpty()) icon = new ItemStack(state.getBlock().asItem());
        String title = state.getBlock().getName().getString();
        String mod = m.cardMod.on() ? ModNames.of(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace()) : null;
        HarvestInfo harvest = m.cardTool.on() ? HarvestReader.read(state, player.getMainHandItem()) : null;
        return new CardData(pos, null, icon, title, mod, harvest, GrowthReader.percent(state), null);
    }
}
