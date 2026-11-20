package dev.henny.vibeyclient.module.waypoints.core.map;

import java.util.Arrays;

public final class TileData {
    public static final short UNKNOWN = Short.MIN_VALUE;
    public static final int PIXELS = TileKey.SIZE * TileKey.SIZE;

    final int[] base;
    final short[] heights;
    private volatile int version;

    public TileData() {
        this(new int[PIXELS], newHeights());
    }

    TileData(int[] base, short[] heights) {
        this.base = base;
        this.heights = heights;
    }

    private static short[] newHeights() {
        short[] h = new short[PIXELS];
        Arrays.fill(h, UNKNOWN);
        return h;
    }

    private int[] shaded;
    private volatile int shadedVersion = -1;

    public int version() {
        return version;
    }

    public int shadedVersion() {
        return shadedVersion;
    }

    public synchronized void publishShaded(int[] pixels, int ofVersion) {
        if (shaded == null) shaded = new int[PIXELS];
        System.arraycopy(pixels, 0, shaded, 0, PIXELS);
        shadedVersion = ofVersion;
    }

    public synchronized int copyShaded(java.nio.IntBuffer out) {
        if (shaded == null) return -1;
        out.put(0, shaded, 0, PIXELS);
        return shadedVersion;
    }

    void bump() {
        version++;
    }

    public synchronized int base(int x, int z) {
        return base[z * TileKey.SIZE + x];
    }

    public synchronized short height(int x, int z) {
        return heights[z * TileKey.SIZE + x];
    }

    public synchronized boolean empty() {
        for (short h : heights) if (h != UNKNOWN) return false;
        return true;
    }

    public synchronized void copyRow(int z, short[] out) {
        System.arraycopy(heights, z * TileKey.SIZE, out, 0, TileKey.SIZE);
    }

    public static int pack(int rgb, int waterDepth) {
        return (Math.min(255, Math.max(0, waterDepth)) << 24) | (rgb & 0xFFFFFF);
    }

    public static int rgb(int packed) {
        return packed & 0xFFFFFF;
    }

    public static int water(int packed) {
        return packed >>> 24;
    }
}
