package dev.henny.cosmetico.thumbs;

import dev.henny.cosmetico.fixtures.Fixtures;
import dev.henny.cosmetico.fixtures.MemoryTextures;
import dev.henny.cosmetico.fixtures.TestPng;
import dev.henny.cosmetico.host.Log;
import dev.henny.cosmetico.host.ThumbnailFetcher;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.registry.Item;
import dev.henny.cosmetico.registry.ItemRegistry;
import dev.henny.cosmetico.registry.ItemSource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThumbnailStoreTest {
    private final ItemRegistry items = new ItemRegistry(() -> Log.NONE);
    private final DevThumbnails dev = new DevThumbnails();
    private final MemoryTextures textures = new MemoryTextures();
    private final List<String> fetched = new ArrayList<>();
    private final ThumbnailFetcher fetcher = url -> {
        fetched.add(url);
        return CompletableFuture.completedFuture(url.contains("broken") ? null : TestPng.of(8, 8));
    };
    private final ThumbnailStore<MemoryTextures.Tex> store = new ThumbnailStore<>(items, dev, textures,
            path -> path.equals("/assets/x/hat.png") ? TestPng.of(4, 4) : null, () -> fetcher, Runnable::run, () -> Log.NONE);
    private final Bundle hat = Fixtures.parse(Fixtures.cosmetic("t:hat", "hat"));

    private Item item(ItemSource src, String thumb) {
        Item i = Item.of(hat, src, 0, thumb, null, 1);
        items.put(i);
        return i;
    }

    @Test
    void builtinThumbnailsWinOverCatalogUrls() {
        item(ItemSource.BUILTIN, "/assets/x/hat.png");
        Item remote = item(ItemSource.REMOTE, "https://cdn/hat.png");
        assertNull(store.get(remote));
        assertNotNull(store.get(remote));
        assertTrue(fetched.isEmpty());
        assertEquals(1, textures.uploaded.size());
    }

    @Test
    void catalogThumbnailsAreFetchedOnceAndFailuresStick() {
        Item ok = item(ItemSource.REMOTE, "https://cdn/hat.png");
        store.get(ok);
        assertNotNull(store.get(ok));
        Item broken = Item.of(Fixtures.parse(Fixtures.cosmetic("t:b", "hat")), ItemSource.REMOTE, 0, "https://cdn/broken.png", null, 1);
        store.get(broken);
        assertNull(store.get(broken));
        assertNull(store.get(broken));
        assertEquals(List.of("https://cdn/hat.png", "https://cdn/broken.png"), fetched);
    }

    @Test
    void devThumbnailsUploadAtOnceAndAreReleasedOnReplace() {
        String key = dev.put("t:hat", TestPng.of(16, 16));
        Item d = item(ItemSource.DEV, key);
        MemoryTextures.Tex first = store.get(d);
        assertNotNull(first);
        dev.put("t:hat", TestPng.of(16, 16));
        assertTrue(first.released);
        assertNull(store.get(d));
    }
}
