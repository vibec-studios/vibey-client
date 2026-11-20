package dev.henny.vibeyclient.module.servers.core;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerListTest {
    @Test
    void addressKeysIgnoreCaseAndDefaultPort() {
        assertEquals("play.example.net", AddressKey.of(" Play.Example.NET:25565 "));
        assertEquals("play.example.net:25566", AddressKey.of("play.example.net:25566"));
        assertEquals("127.0.0.1", AddressKey.of("127.0.0.1"));
    }

    @Test
    void favouritesToggleAndSurviveJson() {
        Favourites f = new Favourites();
        assertTrue(f.toggle("mc.example.org"));
        assertTrue(f.contains("MC.example.org:25565"));
        Favourites back = new Favourites();
        back.read(f.write());
        assertTrue(back.contains("mc.example.org"));
        assertFalse(back.toggle("mc.example.org"));
        assertEquals(0, back.size());
        back.read(JsonParser.parseString("[1, {\"x\":1}, \"a.b\"]"));
        assertEquals(2, back.size());
    }

    @Test
    void filterMatchesNameAddressAndMotd() {
        ServerFilter f = ServerFilter.of("sky", false);
        assertTrue(f.matches("SkyBlock Hub", "hub.example", "", false));
        assertTrue(f.matches("Hub", "sky.example", "", false));
        assertTrue(f.matches("Hub", "hub", "Best Skyblock!", false));
        assertFalse(f.matches("Survival", "s.example", "vanilla", false));
        assertFalse(ServerFilter.of("", true).matches("A", "a", "", false));
        assertTrue(ServerFilter.of("", true).matches("A", "a", "", true));
        assertFalse(ServerFilter.ALL.active());
    }

    @Test
    void favouritesGoFirstStably() {
        List<String> names = List.of("a", "B", "c", "D", "e");
        List<String> out = ServerOrder.arrange(names, n -> !n.equals("e"), n -> Character.isUpperCase(n.charAt(0)), true);
        assertEquals(List.of("B", "D", "a", "c"), out);
        assertTrue(ServerOrder.reordered(names, out));
        assertFalse(ServerOrder.reordered(names, ServerOrder.arrange(names, n -> true, n -> false, true)));
    }

    @Test
    void motdSplitsIntoLimitedLines() {
        List<MotdSegment> segs = List.of(new MotdSegment("Hello ", "#ffffff", false, false),
                new MotdSegment("World\nline two\n\n4\n5", "#ff0000", true, false));
        List<MotdLines.Line> lines = MotdLines.split(segs);
        assertEquals(MotdLines.MAX_LINES, lines.size());
        assertEquals(2, lines.get(0).segs().size());
        assertEquals("line two", lines.get(1).segs().get(0).text());
        assertTrue(lines.get(2).segs().isEmpty());
        assertEquals("Hello World\nline two\n\n4\n5", MotdLines.plain(segs));
    }

    @Test
    void hoverNeedsTheDelay() {
        HoverDelay<String> h = new HoverDelay<>(300);
        long ms = 1_000_000L;
        assertEquals(null, h.update("a", 0));
        assertEquals("a", h.update("a", 300 * ms));
        assertEquals(null, h.update("b", 310 * ms));
        assertEquals(null, h.update(null, 900 * ms));
    }
}
