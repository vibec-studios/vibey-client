package dev.henny.vibeyclient.module.info.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DurabilityTest {
    @Test
    void thresholdHasAMinimum() {
        assertEquals(157, Durability.threshold(1561, 10));
        assertEquals(3, Durability.threshold(59, 1));
        assertEquals(6, Durability.threshold(59, 10));
    }

    @Test
    void levels() {
        assertEquals(WarnLevel.NONE, Durability.level(250, 10, 10));
        assertEquals(WarnLevel.LOW, Durability.level(250, 230, 10));
        assertEquals(WarnLevel.CRITICAL, Durability.level(250, 245, 10));
        assertEquals(WarnLevel.CRITICAL, Durability.level(250, 250, 10));
        assertEquals(WarnLevel.NONE, Durability.level(0, 0, 10));
        assertEquals(0, Durability.remaining(10, 12));
    }

    @Test
    void flashPulsesFasterWhenCritical() {
        assertEquals(0f, Flash.alpha(123, WarnLevel.NONE));
        assertEquals(1f, Flash.alpha(0, WarnLevel.LOW), 1e-4);
        assertEquals(Flash.MIN_ALPHA, Flash.alpha(500, WarnLevel.LOW), 1e-4);
        assertEquals(Flash.MIN_ALPHA, Flash.alpha(200, WarnLevel.CRITICAL), 1e-4);
        assertTrue(Flash.period(WarnLevel.CRITICAL) < Flash.period(WarnLevel.LOW));
    }

    @Test
    void holdGesture() {
        HoldGesture g = new HoldGesture(3);
        assertEquals(HoldGesture.Result.NONE, g.tick(true));
        assertEquals(HoldGesture.Result.TAP, g.tick(false));
        assertEquals(HoldGesture.Result.NONE, g.tick(true));
        assertEquals(HoldGesture.Result.NONE, g.tick(true));
        assertEquals(2f / 3f, g.progress(), 1e-4);
        assertEquals(HoldGesture.Result.HOLD, g.tick(true));
        assertEquals(HoldGesture.Result.NONE, g.tick(true));
        assertEquals(0f, g.progress());
        assertEquals(HoldGesture.Result.NONE, g.tick(false));
        assertEquals(HoldGesture.Result.NONE, g.tick(false));
    }
}
