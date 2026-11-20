package dev.henny.vibeyclient.module.waypoints.map;

import dev.henny.vibeyclient.module.waypoints.core.map.ChunkSample;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

final class ColumnSampler {
    private static final int MAX_SKIP = 24;
    private static final int MAX_WATER = 32;

    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    ChunkSample sample(Level level, LevelChunk chunk, String dimension, int playerY) {
        int cx = chunk.getPos().x, cz = chunk.getPos().z;
        ChunkSample s = new ChunkSample(dimension, cx, cz);
        boolean ceiling = level.dimensionType().hasCeiling();
        int minY = level.getMinY();
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int wx = (cx << 4) + lx, wz = (cz << 4) + lz;
                int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, lx, lz);
                int y = ceiling ? belowCeiling(chunk, wx, wz, Math.min(top, playerY + 2), minY) : top;
                column(level, chunk, s, lx, lz, wx, wz, y, minY);
            }
        }
        return s;
    }

    private void column(Level level, LevelChunk chunk, ChunkSample s, int lx, int lz, int wx, int wz, int y, int minY) {
        BlockState state = chunk.getBlockState(pos.set(wx, y, wz));
        MapColor color = state.getMapColor(level, pos);
        for (int i = 0; color == MapColor.NONE && i < MAX_SKIP && y > minY; i++) {
            y--;
            state = chunk.getBlockState(pos.set(wx, y, wz));
            color = state.getMapColor(level, pos);
        }
        if (color == MapColor.NONE || y <= minY) {
            s.set(lx, lz, 0, 0, 0);
            return;
        }
        if (color == MapColor.WATER) {
            int depth = 0;
            int yy = y;
            while (depth < MAX_WATER && yy > minY && chunk.getBlockState(pos.set(wx, yy, wz)).getMapColor(level, pos) == MapColor.WATER) {
                depth++;
                yy--;
            }
            s.set(lx, lz, MapColor.WATER.col, y, depth);
            return;
        }
        s.set(lx, lz, color.col, y, 0);
    }

    private int belowCeiling(LevelChunk chunk, int wx, int wz, int start, int minY) {
        int y = start;
        while (y > minY && !chunk.getBlockState(pos.set(wx, y, wz)).isAir()) y--;
        while (y > minY && chunk.getBlockState(pos.set(wx, y, wz)).isAir()) y--;
        return y;
    }
}
