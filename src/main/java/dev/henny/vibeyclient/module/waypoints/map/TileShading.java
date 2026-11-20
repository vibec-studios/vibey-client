package dev.henny.vibeyclient.module.waypoints.map;

import dev.henny.vibeyclient.module.waypoints.core.map.PixelOrder;
import dev.henny.vibeyclient.module.waypoints.core.map.TileData;
import dev.henny.vibeyclient.module.waypoints.core.map.TileKey;
import dev.henny.vibeyclient.module.waypoints.core.map.TileShader;

final class TileShading {
    private final int[] scratch = new int[TileData.PIXELS];
    private final short[] north = new short[TileKey.SIZE];

    void shade(TileData tile, TileData northTile) {
        int version = tile.version();
        short[] row = null;
        if (northTile != null) {
            northTile.copyRow(TileKey.SIZE - 1, north);
            row = north;
        }
        TileShader.shade(tile, row, scratch);
        PixelOrder.argbToAbgr(scratch);
        tile.publishShaded(scratch, version);
    }
}
