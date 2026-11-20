package dev.henny.companio.mc.world;

import dev.henny.companio.move.BlockView;
import dev.henny.companio.move.Fluid;
import dev.henny.companio.move.Surface;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class McBlockView implements BlockView {
    private final ClientLevel level;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    public McBlockView(ClientLevel level) {
        this.level = level;
    }

    private BlockState state(int x, int y, int z) {
        return level.getBlockState(pos.set(x, y, z));
    }

    @Override
    public float top(int x, int y, int z) {
        if (y < level.getMinY() || y >= level.getMaxY()) return y < level.getMinY() ? 1f : 0f;
        BlockState s = state(x, y, z);
        if (s.isAir()) return 0f;
        VoxelShape shape = s.getCollisionShape(level, pos);
        if (shape.isEmpty()) return 0f;
        return (float) Math.min(1.5, Math.max(0.0625, shape.max(Direction.Axis.Y)));
    }

    @Override
    public Fluid fluid(int x, int y, int z) {
        FluidState f = state(x, y, z).getFluidState();
        if (f.isEmpty()) return Fluid.NONE;
        if (f.is(FluidTags.LAVA)) return Fluid.LAVA;
        return f.is(FluidTags.WATER) ? Fluid.WATER : Fluid.NONE;
    }

    @Override
    public Surface surface(int x, int y, int z) {
        BlockState s = state(x, y, z);
        if (s.is(BlockTags.ICE)) return Surface.ICE;
        if (s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.SNOW) || s.is(Blocks.POWDER_SNOW)) return Surface.SNOW;
        BlockState above = state(x, y + 1, z);
        return above.is(Blocks.SNOW) ? Surface.SNOW : Surface.NORMAL;
    }

    @Override
    public boolean hazard(int x, int y, int z) {
        BlockState s = state(x, y, z);
        if (s.isAir()) return false;
        return s.is(BlockTags.FIRE) || s.is(Blocks.MAGMA_BLOCK) || s.is(Blocks.CACTUS) || s.is(Blocks.SWEET_BERRY_BUSH)
                || s.is(Blocks.WITHER_ROSE) || s.is(Blocks.POWDER_SNOW) || (s.getBlock() instanceof CampfireBlock && CampfireBlock.isLitCampfire(s));
    }

    @Override
    public boolean loaded(int x, int z) {
        return level.hasChunk(x >> 4, z >> 4);
    }
}
