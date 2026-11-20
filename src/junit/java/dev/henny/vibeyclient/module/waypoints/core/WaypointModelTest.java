package dev.henny.vibeyclient.module.waypoints.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaypointModelTest {
    private final AtomicInteger n = new AtomicInteger();
    private final WaypointList list = new WaypointList(() -> "w" + n.incrementAndGet());

    @Test
    void jsonRoundTrip() {
        list.add(new Waypoint("a", "Home", new Pos(1, 2, 3), Dimensions.OVERWORLD, 0x123456).size(2.5f).visible(false));
        list.markDeath("Death", new Pos(-5, 10, 7), Dimensions.NETHER, 42L);
        List<Waypoint> back = WaypointJson.read(WaypointJson.write(list.all()));
        assertEquals(2, back.size());
        Waypoint death = back.get(0);
        assertEquals(WaypointKind.DEATH, death.kind());
        assertEquals(42L, death.created());
        Waypoint home = back.get(1);
        assertEquals("Home", home.name());
        assertEquals(2.5f, home.size());
        assertFalse(home.visible());
        assertEquals(0x123456, home.color());
    }

    @Test
    void brokenJsonEntriesAreSkipped() {
        String json = "{\"waypoints\":[{\"id\":\"ok\",\"name\":\"A\",\"x\":1,\"y\":2,\"z\":3,\"dim\":\"minecraft:overworld\","
                + "\"color\":\"ffffff\"},{\"id\":\"bad\",\"x\":\"no\"},{\"id\":\"far\",\"name\":\"B\",\"x\":1e9,\"y\":2,"
                + "\"z\":3,\"dim\":\"minecraft:overworld\",\"color\":\"ffffff\"},7]}";
        List<Waypoint> back = WaypointJson.read(json);
        assertEquals(1, back.size());
        assertEquals("ok", back.get(0).id());
        assertTrue(WaypointJson.read("not json {").isEmpty());
    }

    @Test
    void onlyOneDeathMarker() {
        list.markDeath("Death", new Pos(0, 0, 0), Dimensions.OVERWORLD, 1);
        Waypoint second = list.markDeath("Death", new Pos(9, 9, 9), Dimensions.OVERWORLD, 2);
        assertEquals(1, list.size());
        assertEquals(second, list.death());
    }

    @Test
    void nextNameSkipsTaken() {
        list.add(new Waypoint("a", "Waypoint 1", new Pos(0, 0, 0), Dimensions.OVERWORLD, 0));
        list.add(new Waypoint("b", "waypoint 2", new Pos(0, 0, 0), Dimensions.OVERWORLD, 0));
        assertEquals("Waypoint 3", Names.next("Waypoint", list.all()));
    }

    @Test
    void sizeAndColourAreClamped() {
        Waypoint w = new Waypoint("a", "x", new Pos(0, 0, 0), Dimensions.OVERWORLD, 0xFF123456);
        assertEquals(0x123456, w.color());
        assertEquals(Waypoint.MAX_SIZE, w.size(100f).size());
        assertEquals(1f, w.size(Float.NaN).size());
        assertNull(list.remove("missing"));
    }

    @Test
    void netherCoordinates() {
        assertEquals(-2, NetherCoords.toNether(-9));
        assertEquals(1, NetherCoords.toNether(15));
        Pos p = NetherCoords.convert(new Pos(800, 64, -808), Dimensions.OVERWORLD, Dimensions.NETHER);
        assertEquals(new Pos(100, 64, -101), p);
        assertEquals(new Pos(800, 64, -808), NetherCoords.convert(new Pos(100, 64, -101), Dimensions.NETHER, Dimensions.OVERWORLD));
        assertNull(NetherCoords.convert(p, Dimensions.END, Dimensions.NETHER));
        assertEquals(Dimensions.NETHER, NetherCoords.counterpart(Dimensions.OVERWORLD));
        assertNull(NetherCoords.counterpart(Dimensions.END));
    }

    @Test
    void navigation() {
        assertEquals(0f, Navigation.relativeYaw(0f, 0, 10), 1e-3);
        assertEquals(90f, Navigation.relativeYaw(0f, -10, 0), 1e-3);
        assertEquals(-90f, Navigation.relativeYaw(0f, 10, 0), 1e-3);
        assertEquals(-170f, Navigation.wrap(190f), 1e-3);
        assertEquals("999 m", Navigation.distanceText(999.4));
        assertEquals("1.5 km", Navigation.distanceText(1500));
    }
}
