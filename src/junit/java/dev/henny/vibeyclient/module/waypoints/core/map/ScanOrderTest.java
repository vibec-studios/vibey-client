package dev.henny.vibeyclient.module.waypoints.core.map;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScanOrderTest {
    @Test
    void ringsGoOutwardAndCoverEverything() {
        ScanOrder o = new ScanOrder(3);
        assertEquals(49, o.size());
        assertEquals(0, o.dx(0));
        assertEquals(0, o.dz(0));
        Set<Long> seen = new HashSet<>();
        int last = 0;
        for (int i = 0; i < o.size(); i++) {
            int d = o.dx(i) * o.dx(i) + o.dz(i) * o.dz(i);
            assertTrue(d >= last);
            last = d;
            seen.add(((long) o.dx(i) << 32) ^ (o.dz(i) & 0xFFFFFFFFL));
        }
        assertEquals(49, seen.size());
    }

    @Test
    void nearChunksAreRescannedSooner() {
        assertTrue(ScanOrder.due(-1, 0, 9));
        assertTrue(ScanOrder.due(0, 40, 1));
        assertFalse(ScanOrder.due(0, 40, 3));
        assertTrue(ScanOrder.due(0, 400, 3));
        assertFalse(ScanOrder.due(0, 400, 8));
        assertTrue(ScanOrder.due(0, 2400, 8));
    }

    @Test
    void argbToAbgrSwapsRedAndBlue() {
        int[] px = {0xFF112233, 0x80AABBCC, 0};
        PixelOrder.argbToAbgr(px);
        assertArrayEquals(new int[] {0xFF332211, 0x80CCBBAA, 0}, px);
    }

    @Test
    void shadedCopyPublishesVersion() {
        TileData t = new TileData();
        assertEquals(-1, t.shadedVersion());
        int[] px = new int[TileData.PIXELS];
        px[5] = 7;
        t.publishShaded(px, 3);
        java.nio.IntBuffer out = java.nio.IntBuffer.allocate(TileData.PIXELS);
        assertEquals(3, t.copyShaded(out));
        assertEquals(7, out.get(5));
    }
}
