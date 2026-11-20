package dev.henny.vibeyclient.module.cosmetics;

import dev.henny.vibeyclient.module.cosmetics.vui.WheelGeometry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WheelGeometryTest {
    @Test
    void directionsMatchSlots() {
        WheelGeometry g = WheelGeometry.fit(200, 120, 150);
        assertEquals(-1, g.direction(200, 120));
        assertEquals(0, g.direction(200, 40));
        assertEquals(1, g.direction(270, 50));
        assertEquals(2, g.direction(290, 120));
        assertEquals(4, g.direction(200, 200));
        assertEquals(6, g.direction(120, 106));
    }

    @Test
    void slicesDoNotOverlapAndStayInside() {
        for (float d : new float[] {80, 152, 240, 400}) {
            WheelGeometry g = WheelGeometry.fit(300, 200, d);
            for (int i = 0; i < WheelGeometry.SLOTS; i++) {
                int j = (i + 1) % WheelGeometry.SLOTS;
                float dx = g.sliceX(i) - g.sliceX(j), dy = g.sliceY(i) - g.sliceY(j);
                assertTrue(Math.max(Math.abs(dx), Math.abs(dy)) >= g.tile(), "slices " + i + "/" + j + " overlap at " + d);
                assertEquals(i, g.hit(g.sliceX(i) + g.tile() / 2, g.sliceY(i) + g.tile() / 2));
            }
            assertTrue(g.bottom() - g.top() <= d + 1, "fits " + d);
        }
    }
}
