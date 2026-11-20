package dev.henny.vibeyclient.module.cinematic.run;

import dev.henny.vibeyclient.module.cinematic.core.WorldView;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

final class McWorld implements WorldView {
    private final ClientLevel level;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    McWorld(ClientLevel level) {
        this.level = level;
    }

    @Override
    public int tags(int x, int y, int z) {
        if (y < level.getMinY() || y >= level.getMaxY()) return 0;
        pos.set(x, y, z);
        return BlockClass.tags(level, pos, level.getBlockState(pos));
    }

    @Override
    public int kind(int x, int y, int z) {
        BlockState s = level.getBlockState(pos.set(x, y, z));
        return BuiltInRegistries.BLOCK.getId(s.getBlock());
    }

    @Override
    public int surface(int x, int z) {
        if (!loaded(x, z)) return minY();
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
    }

    @Override
    public boolean loaded(int x, int z) {
        return level.hasChunk(x >> 4, z >> 4);
    }

    @Override
    public int minY() {
        return level.getMinY();
    }

    @Override
    public int maxY() {
        return level.getMaxY();
    }
}
