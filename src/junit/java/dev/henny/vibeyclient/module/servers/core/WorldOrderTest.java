package dev.henny.vibeyclient.module.servers.core;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WorldOrderTest {
    static final List<WorldOrder.World> WORLDS = List.of(
            new WorldOrder.World("w1", "Zebra", 100),
            new WorldOrder.World("w2", "apple", 300),
            new WorldOrder.World("w3", "Mango", 200));

    @Test
    void lastPlayedAndName() {
        assertEquals(List.of("w2", "w3", "w1"), WorldOrder.arrange(WORLDS, List.of(), WorldSort.LAST_PLAYED));
        assertEquals(List.of("w2", "w3", "w1"), WorldOrder.arrange(WORLDS, List.of("w1"), WorldSort.NAME));
    }

    @Test
    void customKeepsOrderNewWorldsOnTopMissingDropped() {
        List<String> custom = List.of("gone", "w1", "w3");
        assertEquals(List.of("w2", "w1", "w3"), WorldOrder.arrange(WORLDS, custom, WorldSort.CUSTOM));
        assertEquals(List.of("w2", "w3", "w1"), WorldOrder.arrange(WORLDS, List.of(), WorldSort.CUSTOM));
    }

    @Test
    void dropMovesWithinFullOrderUsingShownAnchors() {
        List<String> full = List.of("a", "b", "c", "d", "e");
        assertEquals(List.of("d", "a", "b", "c", "e"), WorldOrder.drop(full, full, 3, 0));
        assertEquals(List.of("b", "c", "d", "e", "a"), WorldOrder.drop(full, full, 0, 5));
        List<String> shown = List.of("b", "d", "e");
        assertEquals(List.of("a", "e", "b", "c", "d"), WorldOrder.drop(full, shown, 2, 0));
        assertEquals(List.of("a", "c", "d", "b", "e"), WorldOrder.drop(full, shown, 0, 2));
        assertEquals(full, WorldOrder.drop(full, shown, 1, 1));
    }

    @Test
    void sortCycles() {
        assertEquals(WorldSort.LAST_PLAYED, WorldSort.CUSTOM.next());
        assertEquals(WorldSort.CUSTOM, WorldSort.NAME.next());
    }

    @Test
    void codecRoundTripAndBadInput() {
        assertEquals(List.of("a", "b"), OrderCodec.read(OrderCodec.write(List.of("a", "b", "a"))));
        assertEquals(List.of("x"), OrderCodec.read(JsonParser.parseString("{\"order\":[\"x\", 3, \"\", null, {\"a\":1}]}")));
        assertEquals(List.of(), OrderCodec.read(JsonParser.parseString("[1,2]")));
        assertEquals(List.of(), OrderCodec.read(null));
    }

    @Test
    void durations() {
        assertEquals(new Durations.Span(1, Durations.Unit.MINUTES), Durations.of(10_000));
        assertEquals(new Durations.Span(5, Durations.Unit.MINUTES), Durations.left(1_000 + 4 * 60_000 + 30_000, 1_000));
        assertEquals(new Durations.Span(3, Durations.Unit.HOURS), Durations.since(0 + 1, 3 * 3_600_000L + 1));
        assertEquals(new Durations.Span(4, Durations.Unit.DAYS), Durations.of(4 * 86_400_000L + 5));
        assertNull(Durations.since(0, 100));
    }
}
