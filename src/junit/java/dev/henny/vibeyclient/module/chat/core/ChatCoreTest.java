package dev.henny.vibeyclient.module.chat.core;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatCoreTest {
    private static final ZoneId UTC = ZoneOffset.UTC;
    private static final long T = 1_700_000_709_000L;

    @Test
    void timestamps() {
        assertEquals("22:25", Timestamps.format(T, TimeStyle.H24, UTC));
        assertEquals("22:25:09", Timestamps.format(T, TimeStyle.H24_SECONDS, UTC));
        assertEquals("10:25 PM", Timestamps.format(T, TimeStyle.H12, UTC));
        assertEquals("[22:25] ", Timestamps.bracketed(T, TimeStyle.H24, UTC));
    }

    @Test
    void spamCollapse() {
        ChatEntry first = ChatEntry.of("Buy  cheap   ranks ", T, false, null);
        assertTrue(SpamCollapse.repeats(first, "Buy cheap ranks", T + 1000));
        assertFalse(SpamCollapse.repeats(first, "Buy cheap ranks!", T + 1000));
        assertFalse(SpamCollapse.repeats(first, "Buy cheap ranks", T + SpamCollapse.WINDOW_MS + 1));
        assertFalse(SpamCollapse.repeats(null, "x", T));
        ChatEntry third = first.repeated(T + 1).repeated(T + 2);
        assertEquals(3, third.count());
        assertEquals(" ×3", SpamCollapse.suffix(third.count()));
        assertEquals("", SpamCollapse.suffix(1));
    }

    @Test
    void filters() {
        ChatFilters f = ChatFilters.of(List.of("discord.gg", "/b[u]y\\s+now/", "/[broken/", " ", "x".repeat(200)));
        assertEquals(2, f.size());
        assertTrue(f.hides("Join DISCORD.GG/abc"));
        assertTrue(f.hides("BUY   NOW!!"));
        assertFalse(f.hides("hello"));
        assertFalse(ChatFilters.valid("/[broken/"));
        assertTrue(ChatFilters.valid("/ok/"));
        assertFalse(ChatFilters.NONE.hides("anything"));
    }

    @Test
    void search() {
        SearchQuery q = SearchQuery.of("  Dia  ore ");
        assertTrue(q.active());
        assertTrue(q.matches("Found DIAMOND ore at 12 40"));
        assertFalse(q.matches("diamond only"));
        assertEquals(List.of(new Ranges.Range(6, 9), new Ranges.Range(14, 17)), q.ranges("Found DIAMOND ore"));
        assertFalse(SearchQuery.of("   ").active());
        assertTrue(SearchQuery.NONE.matches("x") || !SearchQuery.NONE.active());
    }

    @Test
    void directMessages() {
        assertEquals(new DirectMessage("Alex", "meet at spawn"), DmPatterns.match("[Alex -> me] meet at spawn", "Steve"));
        assertEquals(new DirectMessage("Alex", "hi"), DmPatterns.match("Alex whispers to you: hi", "Steve"));
        assertEquals(new DirectMessage("Alex", "hallo"), DmPatterns.match("Alex flüstert dir zu: hallo", "Steve"));
        assertEquals(new DirectMessage("Alex", "yo"), DmPatterns.match("From Alex: yo", "Steve"));
        assertNull(DmPatterns.match("[Steve -> me] self", "Steve"));
        assertNull(DmPatterns.match("<Alex> whispers are cool", "Steve"));
    }

    @Test
    void historySize() {
        assertEquals(HistorySize.MIN, HistorySize.clamp(3));
        assertEquals(HistorySize.MAX, HistorySize.clamp(1_000_000));
        assertEquals(500, HistorySize.clamp(500));
    }
}
