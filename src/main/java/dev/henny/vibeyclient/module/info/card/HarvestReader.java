package dev.henny.vibeyclient.module.info.card;

import dev.henny.vibeyclient.module.info.core.HarvestInfo;
import dev.henny.vibeyclient.module.info.core.HarvestRules;
import dev.henny.vibeyclient.module.info.core.ToolKind;
import dev.henny.vibeyclient.module.info.core.ToolTier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

public final class HarvestReader {
    private HarvestReader() {}

    public static HarvestInfo read(BlockState s, ItemStack held) {
        EnumSet<ToolKind> kinds = EnumSet.noneOf(ToolKind.class);
        if (s.is(BlockTags.MINEABLE_WITH_PICKAXE)) kinds.add(ToolKind.PICKAXE);
        if (s.is(BlockTags.MINEABLE_WITH_AXE)) kinds.add(ToolKind.AXE);
        if (s.is(BlockTags.MINEABLE_WITH_SHOVEL)) kinds.add(ToolKind.SHOVEL);
        if (s.is(BlockTags.MINEABLE_WITH_HOE)) kinds.add(ToolKind.HOE);
        if (s.is(BlockTags.LEAVES) || s.is(BlockTags.WOOL) || s.is(Blocks.COBWEB)) kinds.add(ToolKind.SHEARS);
        if (s.is(BlockTags.SWORD_EFFICIENT)) kinds.add(ToolKind.SWORD);
        boolean requires = s.requiresCorrectToolForDrops();
        ToolTier tier = HarvestRules.tier(requires, s.is(BlockTags.NEEDS_DIAMOND_TOOL), s.is(BlockTags.NEEDS_IRON_TOOL),
                s.is(BlockTags.NEEDS_STONE_TOOL));
        ToolKind kind = HarvestRules.kind(kinds);
        return HarvestRules.of(kinds, tier, requires, held.isCorrectToolForDrops(s), kind != ToolKind.NONE && kindOf(held) == kind);
    }

    public static ToolKind kindOf(ItemStack st) {
        if (st.is(ItemTags.PICKAXES)) return ToolKind.PICKAXE;
        if (st.is(ItemTags.AXES)) return ToolKind.AXE;
        if (st.is(ItemTags.SHOVELS)) return ToolKind.SHOVEL;
        if (st.is(ItemTags.HOES)) return ToolKind.HOE;
        if (st.is(ItemTags.SWORDS)) return ToolKind.SWORD;
        if (st.is(Items.SHEARS)) return ToolKind.SHEARS;
        return ToolKind.NONE;
    }
}
