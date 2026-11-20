package dev.henny.vibeyclient.module.waypoints.core.map;

public final class ChunkSample {
    public final String dimension;
    public final int chunkX;
    public final int chunkZ;
    public final int[] rgb = new int[256];
    public final short[] height = new short[256];
    public final byte[] water = new byte[256];

    public ChunkSample(String dimension, int chunkX, int chunkZ) {
        this.dimension = dimension;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public void set(int lx, int lz, int rgb, int height, int waterDepth) {
        int i = lz * 16 + lx;
        this.rgb[i] = rgb & 0xFFFFFF;
        this.height[i] = (short) Math.max(-2048, Math.min(4096, height));
        this.water[i] = (byte) Math.min(127, Math.max(0, waterDepth));
    }

    public TileKey tile() {
        return TileKey.ofChunk(dimension, chunkX, chunkZ);
    }
}
