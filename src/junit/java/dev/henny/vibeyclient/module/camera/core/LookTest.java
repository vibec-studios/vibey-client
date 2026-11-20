package dev.henny.vibeyclient.module.camera.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LookTest {
    @Test
    void freelookTurnsAndClampsPitch() {
        LookAngles a = new LookAngles();
        a.reset(30f, 10f);
        a.turn(100, 0);
        assertEquals(45f, a.yaw(), 1e-4);
        a.turn(0, 10_000);
        assertEquals(90f, a.pitch(), 1e-4);
        a.turn(0, -20_000);
        assertEquals(-90f, a.pitch(), 1e-4);
        a.reset(0f, 200f);
        assertEquals(90f, a.pitch(), 1e-4);
        assertEquals(0f, LookAngles.clampPitch(Float.NaN));
    }

    @Test
    void orbitOffsetKeepsTotalPitchInRange() {
        OrbitOffset o = new OrbitOffset();
        assertTrue(o.idle());
        o.drag(200, 1000, 40f);
        assertEquals(30f, o.yaw(0f), 1e-4);
        assertEquals(90f, o.pitch(40f), 1e-4);
        assertEquals(50f, o.pitchOffset(), 1e-4);
        o.drag(0, -100, 40f);
        assertEquals(75f, o.pitch(40f), 1e-4);
        assertFalse(o.idle());
        o.reset();
        assertTrue(o.idle());
    }

    @Test
    void orbitZoomStepsAndClamps() {
        OrbitOffset o = new OrbitOffset();
        o.wheel(-1);
        assertEquals(OrbitOffset.ZOOM_STEP, o.zoom(), 1e-4);
        for (int i = 0; i < 50; i++) o.wheel(-1);
        assertEquals(OrbitOffset.MAX_ZOOM, o.zoom(), 1e-4);
        for (int i = 0; i < 80; i++) o.wheel(1);
        assertEquals(OrbitOffset.MIN_ZOOM, o.zoom(), 1e-4);
    }

    @Test
    void tapVersusDrag() {
        TapOrDrag g = new TapOrDrag();
        g.press(1000);
        g.move(3, 4);
        assertFalse(g.dragging());
        assertTrue(g.release(1100));
        g.press(2000);
        g.move(40, 0);
        assertTrue(g.dragging());
        assertFalse(g.release(2050));
        g.press(3000);
        assertFalse(g.release(3000 + TapOrDrag.MAX_TAP_MS + 1));
        assertFalse(g.release(4000));
    }
}
