package dev.henny.vibeyclient.module.waypoints.core.map;

public record TileKey(String dimension, int tx, int tz) {
    public static final int SHIFT = 7;
    public static final int SIZE = 1 << SHIFT;
    public static final int CHUNKS = SIZE / 16;

    public static TileKey ofBlock(String dimension, int blockX, int blockZ) {
        return new TileKey(dimension, blockX >> SHIFT, blockZ >> SHIFT);
    }

    public static TileKey ofChunk(String dimension, int chunkX, int chunkZ) {
        return new TileKey(dimension, Math.floorDiv(chunkX, CHUNKS), Math.floorDiv(chunkZ, CHUNKS));
    }

    public int minBlockX() {
        return tx << SHIFT;
    }

    public int minBlockZ() {
        return tz << SHIFT;
    }

    public TileKey north() {
        return new TileKey(dimension, tx, tz - 1);
    }

    public String fileName() {
        return tx + "_" + tz + ".vwt";
    }
}
