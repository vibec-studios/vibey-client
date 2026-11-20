package dev.henny.vibeyclient.module.waypoints.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShareCodeTest {
    private final AtomicInteger n = new AtomicInteger();
    private final Supplier<String> ids = () -> "id" + n.incrementAndGet();

    @Test
    void roundTripKeepsEverythingButId() throws Exception {
        Waypoint a = new Waypoint("a", "Home, sweet|home 100%", new Pos(-120, 64, 3500), Dimensions.OVERWORLD, 0x3FE0C5);
        Waypoint b = new Waypoint("b", "Fort", new Pos(12, 40, -7), Dimensions.NETHER, 0xFF8A3B);
        String code = ShareCode.encode(List.of(a, b));
        assertTrue(code.startsWith(ShareCode.PREFIX));
        assertTrue(code.matches("VWP1\\.[A-Za-z0-9_-]+"));
        List<Waypoint> back = ShareCode.decode("look: " + code + " !", ids);
        assertEquals(2, back.size());
        assertEquals("Home, sweet|home 100%", back.get(0).name());
        assertEquals(new Pos(-120, 64, 3500), back.get(0).pos());
        assertEquals(Dimensions.OVERWORLD, back.get(0).dimension());
        assertEquals(0x3FE0C5, back.get(0).color());
        assertEquals(Dimensions.NETHER, back.get(1).dimension());
        assertEquals("id1", back.get(0).id());
    }

    @Test
    void codeIsCompact() {
        Waypoint a = new Waypoint("a", "Base", new Pos(1000, 70, -2000), Dimensions.OVERWORLD, 0x8B6CFF);
        assertTrue(ShareCode.encode(List.of(a)).length() < 48, ShareCode.encode(List.of(a)));
    }

    @Test
    void rejectsGarbage() {
        assertThrows(ShareCode.Invalid.class, () -> ShareCode.decode("hello", ids));
        assertThrows(ShareCode.Invalid.class, () -> ShareCode.decode("VWP1.AAAA", ids));
        String far = ShareCode.PREFIX + java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("99999999999,1,1,o,ffffff,x".getBytes());
        assertThrows(ShareCode.Invalid.class, () -> ShareCode.decode(far, ids));
        String badDim = ShareCode.PREFIX + java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("1,1,1,BAD DIM,ffffff,x".getBytes());
        assertThrows(ShareCode.Invalid.class, () -> ShareCode.decode(badDim, ids));
    }

    @Test
    void namesAreCleaned() throws Exception {
        String code = ShareCode.PREFIX + java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("1,2,3,o,ffffff,§cRed\u0007 name that is far too long for a waypoint label".getBytes());
        Waypoint w = ShareCode.decode(code, ids).get(0);
        assertTrue(w.name().length() <= Waypoint.MAX_NAME);
        assertTrue(w.name().startsWith("cRed name"), w.name());
    }
}
