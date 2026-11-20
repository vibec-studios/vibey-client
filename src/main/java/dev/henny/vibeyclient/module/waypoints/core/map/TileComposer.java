package dev.henny.vibeyclient.module.waypoints.core.map;

public final class TileComposer {
    private TileComposer() {}

    public static boolean apply(TileData tile, ChunkSample s) {
        int ox = Math.floorMod(s.chunkX, TileKey.CHUNKS) * 16;
        int oz = Math.floorMod(s.chunkZ, TileKey.CHUNKS) * 16;
        boolean changed = false;
        synchronized (tile) {
            for (int lz = 0; lz < 16; lz++) {
                int row = (oz + lz) * TileKey.SIZE + ox;
                for (int lx = 0; lx < 16; lx++) {
                    int i = lz * 16 + lx;
                    int packed = s.rgb[i] == 0 ? 0 : TileData.pack(s.rgb[i], s.water[i]);
                    short h = s.rgb[i] == 0 ? TileData.UNKNOWN : s.height[i];
                    if (tile.base[row + lx] != packed || tile.heights[row + lx] != h) {
                        tile.base[row + lx] = packed;
                        tile.heights[row + lx] = h;
                        changed = true;
                    }
                }
            }
            if (changed) tile.bump();
        }
        return changed;
    }
}
