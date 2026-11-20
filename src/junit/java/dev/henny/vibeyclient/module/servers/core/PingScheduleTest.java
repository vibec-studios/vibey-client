package dev.henny.vibeyclient.module.servers.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PingScheduleTest {
    private static final long S = 1_000_000_000L;

    private static List<PingSchedule.Candidate<String>> list() {
        return List.of(new PingSchedule.Candidate<>("a", false, true), new PingSchedule.Candidate<>("b", true, true),
                new PingSchedule.Candidate<>("c", false, true), new PingSchedule.Candidate<>("d", false, false));
    }

    @Test
    void firstSightOnlyStartsTheClock() {
        PingSchedule<String> p = new PingSchedule<>(2, 30_000, 8_000);
        assertTrue(p.due(list(), 0).isEmpty());
        assertTrue(p.due(list(), 29 * S).isEmpty());
        assertEquals(List.of("b", "a"), p.due(list(), 31 * S));
    }

    @Test
    void concurrencyIsBounded() {
        PingSchedule<String> p = new PingSchedule<>(2, 10_000, 8_000);
        p.due(list(), 0);
        List<String> first = p.due(list(), 11 * S);
        first.forEach(k -> p.start(k, 11 * S));
        assertEquals(2, p.inFlight());
        assertTrue(p.due(list(), 12 * S).isEmpty());
        p.done("b", 12 * S);
        assertEquals(List.of("c"), p.due(list(), 12 * S));
    }

    @Test
    void stuckPingsExpire() {
        PingSchedule<String> p = new PingSchedule<>(3, 10_000, 8_000);
        p.start("a", 0);
        assertTrue(p.expired(7 * S).isEmpty());
        assertEquals(List.of("a"), p.expired(8 * S));
        p.forget("a");
        assertEquals(0, p.inFlight());
    }

    @Test
    void cacheExpiresAndEvicts() {
        PingCache<String> c = new PingCache<>(2, 60_000);
        c.put("A.example:25565", "one", 0);
        assertEquals("one", c.get("a.example", 59 * S));
        assertNull(c.get("a.example", 61 * S));
        c.put("x", "1", 0);
        c.put("y", "2", 0);
        c.put("z", "3", 0);
        assertEquals(2, c.size());
        assertNull(c.get("x", 0));
    }
}
