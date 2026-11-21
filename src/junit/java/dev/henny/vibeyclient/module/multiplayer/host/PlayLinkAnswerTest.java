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
                "{\"code\":\"k3x9q2\",\"url\":\"https://axolite.cc/j/k3x9q2\",\"expires_at\":\"2026-11-19T10:00:00.000Z\"}"), "a.axolite.cc");
        assertNotNull(l);
        assertEquals("k3x9q2", l.code());
        assertEquals("https://axolite.cc/j/k3x9q2", l.url());
        assertEquals(java.time.Instant.parse("2026-11-19T10:00:00Z").toEpochMilli(), l.expiresAt());
        assertEquals("a.axolite.cc", l.relayHost());
    }

    /** A backend still on the old domain (or an old link): the url is taken as it comes, nothing is rebuilt. */
    @Test
    void oldDomainUrlKept() {
        PlayLinks.Link l = PlayLinks.parse(JsonParser.parseString(
                "{\"code\":\"k3x9q2\",\"url\":\"https://vibe.henny.dev/j/k3x9q2\"}"), "a.vibey.henny.dev");
        assertNotNull(l);
        assertEquals("https://vibe.henny.dev/j/k3x9q2", l.url());
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

    /** Contract 02: refused with {@code vibe_pack} (older backend 400, pack gone 404) = once more without it. */
    @Test
    void retryWithoutVibePack() {
        com.google.gson.JsonObject with = new com.google.gson.JsonObject();
        with.add("vibe_pack", new com.google.gson.JsonObject());
        com.google.gson.JsonObject without = new com.google.gson.JsonObject();
        var bad = new dev.henny.vibeyclient.api.service.VibeBackend.ApiException(400, "bad_request", "x");
        var gone = new dev.henny.vibeyclient.api.service.VibeBackend.ApiException(404, "not_found", "Pack nicht gefunden");
        var noRoute = new dev.henny.vibeyclient.api.service.VibeBackend.ApiException(404, "http_404", "x");
        var server = new dev.henny.vibeyclient.api.service.VibeBackend.ApiException(500, "internal", "x");
        var offline = new dev.henny.vibeyclient.api.service.VibeBackend.ApiException(0, "network", "x");
        assertTrue(PlayLinks.retryWithoutVibePack(with, bad));
        assertTrue(PlayLinks.retryWithoutVibePack(with, gone));
        assertFalse(PlayLinks.retryWithoutVibePack(with, noRoute), "no play links at all: retrying will not help");
        assertFalse(PlayLinks.retryWithoutVibePack(with, server));
        assertFalse(PlayLinks.retryWithoutVibePack(with, offline));
        assertFalse(PlayLinks.retryWithoutVibePack(without, bad), "second try has no vibe_pack: no loop");
    }
}
