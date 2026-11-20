package dev.henny.vibeyclient.module.visuals.items.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandPoseTest {
    @Test
    void clampsAndWraps() {
        HandPose p = new HandPose(5, -5, 0.2f, 190, -370, Float.NaN, 9);
        assertEquals(1, p.x());
        assertEquals(-1, p.y());
        assertEquals(-170, p.rotX(), 1e-4);
        assertEquals(-10, p.rotY(), 1e-4);
        assertEquals(0, p.rotZ());
        assertEquals(HandPose.MAX_SCALE, p.scale());
        assertEquals(HandPose.MIN_SCALE, new HandPose(0, 0, 0, 0, 0, 0, 0).scale());
    }

    @Test
    void vanillaIsNeutral() {
        assertTrue(HandPose.VANILLA.isVanilla());
        assertFalse(HandPose.VANILLA.moves());
        assertFalse(HandPose.VANILLA.turnsOrScales());
        assertTrue(HandPose.VANILLA.scaled(1.5f).turnsOrScales());
        assertTrue(HandPose.VANILLA.moved(0, 0.1f, 0).moves());
    }

    @Test
    void presetsRoundTrip() {
        for (HandPreset p : HandPreset.values()) {
            assertSame(p, HandPreset.match(p.pose()));
            assertSame(p, HandPreset.byId(p.id()));
        }
        assertNull(HandPreset.match(new HandPose(0.3f, 0.3f, 0, 0, 0, 0, 1)));
        assertSame(HandPreset.VANILLA, HandPreset.match(new HandPose(0.001f, 0, 0, 0.2f, 0, 0, 1)));
    }

    @Test
    void sessionEditsCancelsAndCopies() {
        EditSession s = new EditSession(HandPose.VANILLA, HandPreset.SMALL.pose());
        assertFalse(s.changed());
        s.edit(p -> p.moved(0.1f, 0, 0));
        assertTrue(s.changed());
        assertEquals(0.1f, s.current().x(), 1e-6);
        s.reset();
        assertFalse(s.changed());
        s.select(EditSession.OFF);
        assertSame(HandPreset.SMALL, s.matchingPreset());
        s.copyToOther();
        assertEquals(HandPreset.SMALL.pose(), s.pose(EditSession.MAIN));
        s.presetBoth(HandPreset.LOW);
        assertEquals(HandPreset.LOW.pose(), s.pose(EditSession.MAIN));
        assertEquals(HandPreset.LOW.pose(), s.pose(EditSession.OFF));
    }
}
