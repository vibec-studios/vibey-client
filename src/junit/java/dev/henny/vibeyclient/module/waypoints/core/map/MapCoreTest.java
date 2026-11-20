package dev.henny.vibeyclient.module.waypoints.core.map;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapCoreTest {
    @Test
    void tileKeys() {
        assertEquals(new TileKey("d", -1, 0), TileKey.ofBlock("d", -1, 127));
        assertEquals(new TileKey("d", 1, -1), TileKey.ofBlock("d", 128, -1));
        assertEquals(new TileKey("d", -1, 0), TileKey.ofChunk("d", -1, 7));
        assertEquals(-128, new TileKey("d", -1, 0).minBlockX());
    }

    @Test
    void composeShadeAndCodec() throws Exception {
        TileData tile = new TileData();
        assertTrue(tile.empty());
        ChunkSample s = new ChunkSample("d", -1, 1);
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) s.set(x, z, 0x7FB238, 64 + z, 0);
        s.set(3, 3, 0x4040FF, 62, 5);
        s.set(5, 5, 0, 0, 0);
        assertTrue(TileComposer.apply(tile, s));
        assertFalse(TileComposer.apply(tile, s));
        int ox = 7 * 16, oz = 16;
        assertEquals(64, tile.height(ox, oz));
        int[] argb = new int[TileData.PIXELS];
        TileShader.shade(tile, null, argb);
        assertEquals(0xFF000000 | ColumnShade.scale(0x7FB238, ColumnShade.LEVEL), argb[oz * TileKey.SIZE + ox]);
        assertEquals(0xFF000000 | ColumnShade.scale(0x7FB238, ColumnShade.HIGHER), argb[(oz + 1) * TileKey.SIZE + ox]);
        assertEquals(0, argb[(oz + 5) * TileKey.SIZE + ox + 5]);
        assertEquals(0, argb[0]);
        int water = argb[(oz + 3) * TileKey.SIZE + ox + 3];
        assertTrue((water & 0xFF) > ((water >> 16) & 0xFF));
        TileData back = TileCodec.decode(TileCodec.encode(tile));
        int[] argb2 = new int[TileData.PIXELS];
        TileShader.shade(back, null, argb2);
        assertArrayEquals(argb, argb2);
        assertTrue(TileCodec.encode(tile).length < 8 * 1024);
    }

    @Test
    void shadeUsesNorthRow() {
        TileData tile = new TileData();
        ChunkSample s = new ChunkSample("d", 0, 0);
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) s.set(x, z, 0x808080, 70, 0);
        TileComposer.apply(tile, s);
        short[] north = new short[TileKey.SIZE];
        java.util.Arrays.fill(north, (short) 80);
        int[] argb = new int[TileData.PIXELS];
        TileShader.shade(tile, north, argb);
        assertEquals(0xFF000000 | ColumnShade.scale(0x808080, ColumnShade.LOWER), argb[0]);
    }

    @Test
    void cachePolicyEvictsOldestUntilUnderLimit() {
        List<CacheFile> files = List.of(new CacheFile("a", 100, 3), new CacheFile("b", 100, 1),
                new CacheFile("c", 100, 2), new CacheFile("d", 100, 4));
        assertTrue(TileCachePolicy.evict(files, 400, Set.of()).isEmpty());
        List<CacheFile> gone = TileCachePolicy.evict(files, 250, Set.of("b"));
        assertEquals(List.of("c", "a"), gone.stream().map(CacheFile::path).toList());
    }

    @Test
    void lruEvictsLeastRecentlyTouched() {
        LruSet<String> lru = new LruSet<>(2);
        assertTrue(lru.touch("a").isEmpty());
        assertTrue(lru.touch("b").isEmpty());
        lru.touch("a");
        assertEquals(List.of("b"), lru.touch("c"));
        assertTrue(lru.contains("a"));
        assertEquals(2, lru.size());
    }

    @Test
    void mapViewZoomKeepsPointUnderCursor() {
        MapView v = new MapView().size(400, 200).center(100, -50).zoom(1);
        double wx = v.worldX(300), wz = v.worldZ(20);
        v.zoomAt(300, 20, 2);
        assertEquals(2, v.zoom(), 1e-9);
        assertEquals(wx, v.worldX(300), 1e-9);
        assertEquals(wz, v.worldZ(20), 1e-9);
        v.zoom(100);
        assertEquals(MapView.MAX_ZOOM, v.zoom(), 1e-9);
        v.center(100, -50).zoom(1).pan(10, -20);
        assertEquals(90, v.centerX(), 1e-9);
        assertEquals(-30, v.centerZ(), 1e-9);
        assertEquals(Math.floorDiv(-110, 128), v.minTileX());
    }

    @Test
    void minimapProjection() {
        float[] out = new float[2];
        assertTrue(MinimapMath.project(0, 10, MinimapMath.rotation(0, true), 1, 50, out));
        assertEquals(0, out[0], 1e-4);
        assertEquals(-10, out[1], 1e-4);
        MinimapMath.project(-10, 0, MinimapMath.rotation(90, true), 1, 50, out);
        assertEquals(-10, out[1], 1e-4);
        MinimapMath.project(5, -7, MinimapMath.rotation(123, false), 2, 50, out);
        assertEquals(10, out[0], 1e-4);
        assertEquals(-14, out[1], 1e-4);
        assertFalse(MinimapMath.project(400, 100, 0, 1, 50, out));
        assertEquals(50, out[0], 1e-4);
        assertEquals(12.5, out[1], 1e-4);
    }
}
