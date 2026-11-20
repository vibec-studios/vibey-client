package dev.henny.vibeyclient.module.visuals.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemTumbleTest {
    private static final float PI = (float) Math.PI;

    @Test
    void spinsWhileFallingFasterWhenFaster() {
        ItemTumble slow = new ItemTumble(1), fast = new ItemTumble(1);
        slow.step(0.05f, false, false, 0.05f, true);
        fast.step(0.05f, false, false, 0.3f, true);
        assertTrue(fast.angle() > slow.angle());
        assertTrue(slow.angle() > 0);
        assertFalse(fast.settled());
    }

    @Test
    void waterSlowsTheSpin() {
        ItemTumble air = new ItemTumble(2), water = new ItemTumble(2);
        air.step(0.05f, false, false, 0.2f, true);
        water.step(0.05f, false, true, 0.2f, true);
        assertEquals(air.angle() * ItemTumble.FLUID_FACTOR, water.angle(), 1e-5);
    }

    @Test
    void settlesFlatOnTheGround() {
        ItemTumble t = new ItemTumble(3);
        for (int i = 0; i < 7; i++) t.step(0.05f, false, false, 0.2f, true);
        for (int i = 0; i < 60; i++) t.step(0.05f, true, false, 0f, true);
        assertTrue(t.settled());
        float r = t.angle() % PI;
        assertTrue(r < 1e-3 || PI - r < 1e-3, "flat rest angle " + t.angle());
    }

    @Test
    void blocksRestOnAnySide() {
        assertEquals(PI / 2, ItemTumble.restAngle(1.4f, false), 1e-5);
        assertEquals(PI, ItemTumble.restAngle(1.7f, true), 1e-5);
        assertEquals(0f, ItemTumble.restAngle(1.4f, true), 1e-5);
    }

    @Test
    void hugeFrameGapsAreClamped() {
        ItemTumble a = new ItemTumble(4), b = new ItemTumble(4);
        a.step(5f, false, false, 1f, false);
        b.step(ItemTumble.MAX_DT, false, false, 1f, false);
        assertEquals(b.angle(), a.angle(), 1e-6);
    }

    @Test
    void seedsGiveDifferentYaw() {
        assertNotEquals(new ItemTumble(1).yaw(), new ItemTumble(2).yaw());
        assertEquals(new ItemTumble(7).yaw(), new ItemTumble(7).yaw());
    }

    @Test
    void liftRestsTheModelOnTheGround() {
        assertEquals(0.25f + ItemLift.GAP, ItemLift.lift(0f, 0.5f, 0.03f), 1e-5);
        assertEquals(0.015f + ItemLift.GAP, ItemLift.lift(-PI / 2, 0.5f, 0.03f), 1e-5);
        assertTrue(ItemLift.flat(0.03f));
        assertFalse(ItemLift.flat(0.25f));
        assertEquals(0.03f + 0.03f * 1.5f * 2, ItemLift.stackDepth(0.03f, 3), 1e-6);
        assertEquals(0.25f, ItemLift.stackDepth(0.25f, 3), 1e-6);
    }
}
