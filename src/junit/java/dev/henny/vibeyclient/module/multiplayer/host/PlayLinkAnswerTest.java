package dev.henny.vibeyclient.module.multiplayer.host;

import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The backend's answer to {@code POST /api/v1/play-links} (PLAY-LINKS.md §3): {@code {code, url, expires_at}}. */
class PlayLinkAnswerTest {
    @Test
    void isoExpiry() {
        PlayLinks.Link l = PlayLinks.parse(JsonParser.parseString(
                "{\"code\":\"k3x9q2\",\"url\":\"https://vibe.henny.dev/j/k3x9q2\",\"expires_at\":\"2026-11-19T10:00:00.000Z\"}"), "a.vibey.henny.dev");
        assertNotNull(l);
        assertEquals("k3x9q2", l.code());
        assertEquals("https://vibe.henny.dev/j/k3x9q2", l.url());
        assertEquals(java.time.Instant.parse("2026-11-19T10:00:00Z").toEpochMilli(), l.expiresAt());
        assertEquals("a.vibey.henny.dev", l.relayHost());
    }

    @Test
    void numericAndOffsetExpiry() {
        assertEquals(1795040000_000L, PlayLinks.expiry(new JsonPrimitive(1795040000L)));
        assertEquals(1795040000_123L, PlayLinks.expiry(new JsonPrimitive(1795040000_123L)));
        assertEquals(java.time.Instant.parse("2026-11-19T09:00:00Z").toEpochMilli(), PlayLinks.expiry(new JsonPrimitive("2026-11-19T10:00:00+01:00")));
        assertEquals(0, PlayLinks.expiry(new JsonPrimitive("tomorrow")));
        assertEquals(0, PlayLinks.expiry(null));
    }

    @Test
    void brokenAnswers() {
        assertNull(PlayLinks.parse(null, "h"));
        assertNull(PlayLinks.parse(JsonParser.parseString("[]"), "h"));
        assertNull(PlayLinks.parse(JsonParser.parseString("{\"code\":\"k3x9q2\"}"), "h"), "no url");
        assertNull(PlayLinks.parse(JsonParser.parseString("{\"code\":\"../x\",\"url\":\"u\"}"), "h"), "code used in a path");
        PlayLinks.Link noExpiry = PlayLinks.parse(JsonParser.parseString("{\"code\":\"abcdef\",\"url\":\"u\"}"), "h");
        assertNotNull(noExpiry);
        assertEquals(0, noExpiry.expiresAt());
    }
}
