package dev.henny.vibeyclient.backend;

import com.google.gson.JsonParser;
import dev.henny.vibeyclient.backend.Notices.Notice;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** {@code GET /api/v1/announcements} (vibe-launcher docs/DOMAINS.md): parsing and the visibility rules. */
class NoticesTest {
    private static final long NOW = Instant.parse("2026-11-19T12:00:00Z").toEpochMilli();

    @Test
    void contractExample() {
        List<Notice> l = parse("{ \"items\": [ { \"id\": \"domain-move-2026-11\", \"level\": \"info\", \"title\": \"Vibe zieht um\","
                + " \"body\": \"Neue Adressen …\", \"url\": \"https://axolite.cc\", \"until\": \"2027-03-01T00:00:00Z\","
                + " \"apps\": [\"launcher\", \"client\"], \"max_version\": null } ] }");
        assertEquals(1, l.size());
        Notice n = l.get(0);
        assertEquals("domain-move-2026-11", n.id());
        assertEquals(Notices.Level.INFO, n.level());
        assertEquals("Vibe zieht um", n.title());
        assertEquals("https://axolite.cc", n.url());
        assertEquals(Instant.parse("2027-03-01T00:00:00Z").toEpochMilli(), n.until());
        assertNull(n.maxVersion());
        assertTrue(Notices.visible(n, NOW, "0.3.0"));
    }

    @Test
    void untilAppsAndMaxVersion() {
        List<Notice> l = parse("{\"items\":["
                + "{\"id\":\"expired\",\"title\":\"t\",\"until\":\"2026-11-01T00:00:00Z\"},"
                + "{\"id\":\"launcher-only\",\"title\":\"t\",\"apps\":[\"launcher\"]},"
                + "{\"id\":\"old-clients\",\"title\":\"t\",\"max_version\":\"0.2.9\"},"
                + "{\"id\":\"up-to-ours\",\"title\":\"t\",\"max_version\":\"0.3.0\"},"
                + "{\"id\":\"all\",\"title\":\"t\",\"level\":\"warn\",\"apps\":[]}]}");
        assertEquals(5, l.size());
        assertFalse(Notices.visible(l.get(0), NOW, "0.3.0"), "until in the past");
        assertTrue(Notices.visible(l.get(0), Instant.parse("2026-10-01T00:00:00Z").toEpochMilli(), "0.3.0"));
        assertFalse(Notices.visible(l.get(1), NOW, "0.3.0"), "not for the client");
        assertFalse(Notices.visible(l.get(2), NOW, "0.3.0"), "max_version lower than ours");
        assertTrue(Notices.visible(l.get(2), NOW, "0.2.1"));
        assertTrue(Notices.visible(l.get(3), NOW, "0.3.0"), "max_version equal");
        assertTrue(Notices.visible(l.get(4), NOW, "0.3.0"));
        assertEquals(Notices.Level.WARN, l.get(4).level());
    }

    @Test
    void badItemsSkipped() {
        List<Notice> l = parse("{\"items\":["
                + "{\"title\":\"no id\"},"
                + "{\"id\":\"no-title\"},"
                + "{\"id\":\"bad id/..\",\"title\":\"t\"},"
                + "{\"id\":\"bad-until\",\"title\":\"t\",\"until\":\"soon\"},"
                + "{\"id\":\"js\",\"title\":\"t\",\"url\":\"javascript:alert(1)\"},"
                + "{\"id\":\"dup\",\"title\":\"first\"},{\"id\":\"dup\",\"title\":\"second\"},"
                + "\"text\", 5, null]}");
        assertEquals(2, l.size());
        assertEquals("js", l.get(0).id());
        assertNull(l.get(0).url(), "only http(s) links");
        assertEquals("first", l.get(1).title());
        assertTrue(Notices.parse(JsonParser.parseString("[]")).isEmpty());
        assertTrue(Notices.parse(JsonParser.parseString("{\"items\":{}}")).isEmpty());
        assertTrue(Notices.parse(null).isEmpty());
    }

    @Test
    void textCleaned() {
        StringBuilder longBody = new StringBuilder();
        for (int i = 0; i < 100; i++) longBody.append("word ");
        Notice n = parse("{\"items\":[{\"id\":\"x\",\"title\":\"\\u00a7cRed\\u0007 title\",\"body\":\"" + longBody + longBody + "\"}]}").get(0);
        assertEquals("Red title", n.title());
        assertTrue(n.body().length() <= Notices.MAX_BODY);
        assertTrue(n.body().endsWith("…"));
        List<Notice> many = parse("{\"items\":[" + String.join(",", java.util.stream.IntStream.range(0, 30).mapToObj(i -> "{\"id\":\"a" + i + "\",\"title\":\"t\"}").toList()) + "]}");
        assertEquals(Notices.MAX_ITEMS, many.size());
    }

    @Test
    void versions() {
        assertTrue(Notices.compareVersions("0.10.0", "0.9.2") > 0);
        assertEquals(0, Notices.compareVersions("0.3", "0.3.0"));
        assertEquals(0, Notices.compareVersions("v0.3.0", "0.3.0-beta.1"));
        assertTrue(Notices.compareVersions("0.2.1", "0.3.0") < 0);
        assertTrue(Notices.compareVersions("1.0.0", "0.99.99") > 0);
    }

    private static List<Notice> parse(String s) {
        return Notices.parse(JsonParser.parseString(s));
    }
}
