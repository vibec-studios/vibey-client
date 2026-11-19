package dev.henny.vibeyclient.backend.net;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletionException;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** ETag revalidation + disk cache of {@link HttpCache}, gzip decoding of {@link Net}, against a local server. */
class HttpCacheTest {
    static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void resolveDecidesWhatToUse() throws Exception {
        HttpCache.Entry cached = new HttpCache.Entry(utf8("old"), "W/\"v1\"");
        HttpCache.Result nm = HttpCache.resolve(cached, 304, null, new byte[0]);
        assertTrue(nm.notModified());
        assertArrayEquals(utf8("old"), nm.body());
        assertEquals("W/\"v1\"", nm.etag());
        HttpCache.Result fresh = HttpCache.resolve(cached, 200, "\"v2\"", utf8("new"));
        assertFalse(fresh.notModified());
        assertEquals("\"v2\"", fresh.etag());
        assertThrows(HttpCache.StatusException.class, () -> HttpCache.resolve(null, 304, "\"v\"", new byte[0]));
        assertEquals(503, assertThrows(HttpCache.StatusException.class, () -> HttpCache.resolve(cached, 503, null, new byte[0])).status);
    }

    @Test
    void keepsTheOldFileLayout(@TempDir Path dir) throws Exception {
        // caches written by older clients: catalog.json + catalog.etag
        Files.writeString(dir.resolve("catalog.json"), "{\"items\":[]}");
        Files.writeString(dir.resolve("catalog.etag"), "\"abc\"\n");
        HttpCache c = new HttpCache(dir);
        HttpCache.Entry e = c.load("catalog");
        assertEquals("\"abc\"", e.etag());
        assertEquals("{\"items\":[]}", new String(e.body(), StandardCharsets.UTF_8));
        c.store("catalog", utf8("{}"), null); // no ETag: the old one must not stay next to a new body
        assertNull(c.load("catalog").etag());
        assertNull(new HttpCache(dir.resolve("missing")).load("catalog"));
    }

    @Test
    void revalidatesWithIfNoneMatchAndStores(@TempDir Path dir) throws Exception {
        try (LocalServer s = new LocalServer()) {
            s.on("/cat", (ex, n) -> {
                String inm = ex.getRequestHeaders().getFirst("If-None-Match");
                if ("W/\"v1\"".equals(inm)) LocalServer.send(ex, 304, null, "ETag", "W/\"v1\"");
                else LocalServer.send(ex, 200, "{\"version\":\"v1\"}", "ETag", "W/\"v1\"");
            });
            HttpCache c = new HttpCache(dir);
            java.util.concurrent.Executor direct = Runnable::run;
            HttpCache.Result first = c.fetch("catalog", s.base() + "/cat", c.load("catalog"), Net.Kind.API, direct).join();
            assertFalse(first.notModified());
            assertEquals("W/\"v1\"", Files.readString(dir.resolve("catalog.etag")));
            HttpCache.Result second = c.fetch("catalog", s.base() + "/cat", c.load("catalog"), Net.Kind.API, direct).join();
            assertTrue(second.notModified());
            assertEquals("{\"version\":\"v1\"}", new String(second.body(), StandardCharsets.UTF_8));
            assertEquals(2, s.hits("/cat"));
            s.on("/gone", (ex, n) -> LocalServer.send(ex, 404, "{}"));
            CompletionException err = assertThrows(CompletionException.class,
                    () -> c.fetch("x", s.base() + "/gone", null, Net.Kind.API, direct).join());
            assertEquals(404, ((HttpCache.StatusException) Net.cause(err)).status);
        }
    }

    @Test
    void decodesGzipAndSendsAcceptEncoding() throws Exception {
        String json = "{\"items\":[" + "{\"id\":\"x\"},".repeat(500) + "{}]}";
        ByteArrayOutputStream gz = new ByteArrayOutputStream();
        try (GZIPOutputStream o = new GZIPOutputStream(gz)) {
            o.write(utf8(json));
        }
        try (LocalServer s = new LocalServer()) {
            s.on("/gz", (ex, n) -> {
                if (!"gzip".equals(ex.getRequestHeaders().getFirst("Accept-Encoding"))) {
                    LocalServer.send(ex, 400, "no gzip asked");
                    return;
                }
                ex.getResponseHeaders().add("Content-Encoding", "gzip");
                ex.sendResponseHeaders(200, gz.size());
                ex.getResponseBody().write(gz.toByteArray());
            });
            Net.Response r = Net.send(Net.request(s.base() + "/gz", Net.Kind.API).GET().build()).join();
            assertEquals(200, r.status());
            assertEquals(json, r.text());
        }
    }

    @Test
    void refusesHugeOrUnknownBodies() {
        assertThrows(Net.BodyException.class, () -> Net.decode(new java.io.ByteArrayInputStream(new byte[1]), "br"));
        byte[] zeros;
        try {
            ByteArrayOutputStream gz = new ByteArrayOutputStream();
            try (GZIPOutputStream o = new GZIPOutputStream(gz)) {
                o.write(new byte[Net.MAX_BODY + 1]);
            }
            zeros = gz.toByteArray();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        byte[] bomb = zeros;
        assertThrows(Net.BodyException.class, () -> Net.decode(new java.io.ByteArrayInputStream(bomb), "gzip"));
    }
}
