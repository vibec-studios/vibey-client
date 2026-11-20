package dev.henny.vibeyclient.module.inventory.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacingTest {
    private static final long MS = 1_000_000L;

    @Test
    void neverMoreClicksThanTheRate() {
        ClickPacer p = new ClickPacer(10);
        int sent = 0;
        for (long t = 0; t < 1000 * MS; t += MS) if (p.take(t)) sent++;
        assertEquals(10, sent);
    }

    @Test
    void rateIsClamped() {
        assertEquals(ClickPacer.MAX_RATE, ClickPacer.clamp(500));
        assertEquals(ClickPacer.MIN_RATE, ClickPacer.clamp(0));
        assertEquals(1_000_000_000L / 20, new ClickPacer(99).gapNanos());
    }

    @Test
    void firstClickIsImmediate() {
        ClickPacer p = new ClickPacer(12);
        assertTrue(p.take(5));
        assertFalse(p.take(6));
        assertTrue(p.take(5 + 84 * MS));
    }

    @Test
    void queueIsBounded() {
        BoundedQueue<Integer> q = new BoundedQueue<>(2);
        assertTrue(q.offer(1));
        assertTrue(q.offer(2));
        assertFalse(q.offer(3));
        assertEquals(1, q.poll());
        assertEquals(1, q.size());
    }

    @Test
    void dragVisitsEachSlotOnce() {
        DragTracker d = new DragTracker();
        SlotView a = new SlotView(1, Section.CONTAINER, "stone", 5, 64, true);
        SlotView b = new SlotView(2, Section.CONTAINER, "dirt", 5, 64, true);
        d.start(1, "stone", false);
        assertFalse(d.visit(a));
        assertTrue(d.visit(b));
        assertFalse(d.visit(b));
        d.stop();
        assertFalse(d.visit(b));
    }

    @Test
    void matchingDragSkipsOtherItems() {
        DragTracker d = new DragTracker();
        d.start(1, "stone", true);
        assertFalse(d.visit(new SlotView(2, Section.MAIN, "dirt", 5, 64, true)));
        assertTrue(d.visit(new SlotView(3, Section.MAIN, "stone", 5, 64, true)));
        assertFalse(d.visit(new SlotView(4, Section.MAIN, "", 0, 64, true)));
    }

    @Test
    void keyGestureTellsTapFromHold() {
        KeyGesture g = new KeyGesture(250);
        g.update(true, 0);
        assertEquals(KeyGesture.Result.TAP, g.update(false, 100 * MS));
        g.update(true, 0);
        assertTrue(g.holding(300 * MS));
        assertEquals(KeyGesture.Result.HOLD_END, g.update(false, 300 * MS));
        g.update(true, 0);
        g.markUsed();
        assertEquals(KeyGesture.Result.HOLD_END, g.update(false, 10 * MS));
    }

    @Test
    void durabilityPercent() {
        assertEquals(100, DurabilityText.percent(250, 0));
        assertEquals(1, DurabilityText.percent(1561, 1560));
        assertEquals(0, DurabilityText.percent(250, 250));
        assertEquals(50, DurabilityText.percent(250, 125));
        assertEquals(DurabilityText.CRITICAL, DurabilityText.color(5));
        assertEquals(DurabilityText.GOOD, DurabilityText.color(90));
    }
}
