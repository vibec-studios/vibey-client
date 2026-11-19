package dev.henny.cosmetico.load;

import dev.henny.cosmetico.fixtures.Fixtures;
import dev.henny.cosmetico.fixtures.TestEngine;
import dev.henny.cosmetico.registry.Item;
import dev.henny.cosmetico.registry.ItemSource;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexLoaderTest {
    private final TestEngine t = new TestEngine();

    @Test
    void readsTheIndexAndSkipsBrokenBundles() {
        String index = "{\"items\":[{\"file\":\"a.vibey\",\"thumbnail\":\"a.png\",\"sort\":5},{\"file\":\"bad.vibey\"},{\"file\":\"gone.vibey\"}]}";
        Map<String, byte[]> files = Map.of("/r/index.json", index.getBytes(StandardCharsets.UTF_8),
                "/r/a.vibey", Fixtures.cosmetic("t:a", "hat").toString().getBytes(StandardCharsets.UTF_8),
                "/r/bad.vibey", "{".getBytes(StandardCharsets.UTF_8));
        Map<String, Item> items = t.engine.indexLoader().read(files::get, "/r/", ItemSource.BUILTIN);
        assertEquals(1, items.size());
        Item a = items.get("t:a");
        assertEquals("/r/a.png", a.thumbnail());
        assertEquals(5, a.sort());
        assertEquals("default", a.availability());
    }

    @Test
    void missingIndexLoadsNothing() {
        assertTrue(t.engine.indexLoader().read(p -> null, "/none/", ItemSource.BUILTIN).isEmpty());
    }
}
