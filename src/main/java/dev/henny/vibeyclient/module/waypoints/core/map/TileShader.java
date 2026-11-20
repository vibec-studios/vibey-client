package dev.henny.vibeyclient.module.waypoints.core.map;

public final class TileShader {
    private TileShader() {}

    public static void shade(TileData tile, short[] northRow, int[] out) {
        int n = TileKey.SIZE;
        synchronized (tile) {
            for (int z = 0; z < n; z++) {
                int row = z * n;
                for (int x = 0; x < n; x++) {
                    short north = z > 0 ? tile.heights[row - n + x] : northRow == null ? TileData.UNKNOWN : northRow[x];
                    out[row + x] = ColumnShade.shade(tile.base[row + x], tile.heights[row + x], north);
                }
            }
        }
    }
}
