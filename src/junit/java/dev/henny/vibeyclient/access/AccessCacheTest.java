package dev.henny.vibeyclient.access;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessCacheTest {
    private static final long NOW = 1_800_000_000_000L;

    @TempDir
    Path dir;

    @Test
    void survivesARestartPerAccount() {
        Path f = dir.resolve("vibeyclient").resolve("access.json");
        new AccessCache(f).remember("a", NOW);
        AccessCache again = new AccessCache(f);
        assertTrue(again.fresh("a", NOW + 1000));
        assertFalse(again.fresh("b", NOW));
        again.forget("a");
        assertFalse(new AccessCache(f).fresh("a", NOW));
    }

    @Test
    void sevenDaysExactly() {
        AccessCache c = new AccessCache(dir.resolve("access.json"));
        c.remember("a", NOW);
        assertTrue(c.fresh("a", NOW + AccessCache.TTL_MS - 1));
        assertFalse(c.fresh("a", NOW + AccessCache.TTL_MS));
    }

    @Test
    void timestampsFromTheFutureDoNotCount() {
        AccessCache c = new AccessCache(dir.resolve("access.json"));
        c.remember("a", NOW + AccessCache.SKEW_MS + 1);
        assertFalse(c.fresh("a", NOW));
    }

    @Test
    void brokenFileMeansNoCache() throws Exception {
        Path f = dir.resolve("access.json");
        Files.writeString(f, "{ not json");
        AccessCache c = new AccessCache(f);
        assertFalse(c.fresh("a", NOW));
        c.remember("a", NOW);
        assertTrue(new AccessCache(f).fresh("a", NOW));
    }
}
