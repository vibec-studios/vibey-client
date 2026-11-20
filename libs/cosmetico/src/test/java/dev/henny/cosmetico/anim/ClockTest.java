package dev.henny.cosmetico.anim;

import dev.henny.cosmetico.fixtures.FakeClock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClockTest {
    @Test
    void pinnedTimeWinsUntilUnpinned() {
        FakeClock fake = new FakeClock();
        Clock clock = new Clock(fake);
        fake.advance(2.0);
        assertEquals(2.0, clock.now(), 1e-9);
        clock.pin(7.5);
        fake.advance(1.0);
        assertTrue(clock.pinned());
        assertEquals(7.5, clock.now(), 0.0);
        clock.pin(Double.NaN);
        assertFalse(clock.pinned());
        assertEquals(3.0, clock.now(), 1e-9);
    }
}
