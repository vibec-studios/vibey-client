package dev.henny.vibeyclient.module.cinematic.run;

import dev.henny.vibeyclient.module.cinematic.core.WorldView;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

final class BlockClass {
    private BlockClass() {}

    static int tags(BlockGetter level, BlockPos pos, BlockState s) {
        if (s.isAir()) return 0;
        int t = 0;
        boolean water = s.getFluidState().is(FluidTags.WATER);
        if (water) t |= WorldView.WATER;
        if (s.getFluidState().is(FluidTags.LAVA)) t |= WorldView.SOLID;
        boolean collides = !s.getCollisionShape(level, pos).isEmpty();
        if (collides || s.canOcclude()) t |= WorldView.SOLID;
        if (s.canOcclude()) t |= WorldView.OPAQUE;
        if (s.getLightEmission() > 0) t |= WorldView.LIGHT;
        if (natural(s)) t |= WorldView.NATURAL;
        else if (collides && !water) t |= WorldView.BUILT;
        return t;
    }

    private static boolean natural(BlockState s) {
        return s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(BlockTags.LEAVES)
                || s.is(BlockTags.LOGS) || s.is(BlockTags.SNOW) || s.is(BlockTags.ICE) || s.is(BlockTags.TERRACOTTA)
                || s.is(BlockTags.BASE_STONE_NETHER) || s.is(BlockTags.NYLIUM) || s.is(BlockTags.CORAL_BLOCKS)
                || s.is(Blocks.GRAVEL) || s.is(Blocks.CLAY) || s.is(Blocks.SANDSTONE) || s.is(Blocks.RED_SANDSTONE)
                || s.is(Blocks.BEDROCK) || s.is(Blocks.OBSIDIAN) || s.is(Blocks.END_STONE) || s.is(Blocks.MAGMA_BLOCK)
                || s.is(Blocks.SOUL_SAND) || s.is(Blocks.SOUL_SOIL) || s.is(Blocks.POWDER_SNOW) || s.is(Blocks.MOSS_BLOCK)
                || s.is(Blocks.BROWN_MUSHROOM_BLOCK) || s.is(Blocks.RED_MUSHROOM_BLOCK) || s.is(Blocks.MUSHROOM_STEM)
                || s.is(BlockTags.COAL_ORES) || s.is(BlockTags.IRON_ORES) || s.is(BlockTags.COPPER_ORES)
                || s.is(Blocks.CACTUS) || s.is(Blocks.PUMPKIN) || s.is(Blocks.MELON) || s.is(Blocks.BAMBOO);
    }
}
