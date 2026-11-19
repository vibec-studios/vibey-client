package dev.henny.cosmetico;

import dev.henny.cosmetico.fixtures.Fixtures;
import dev.henny.cosmetico.fixtures.TestEngine;
import dev.henny.cosmetico.host.BundleResources;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.registry.Item;
import dev.henny.cosmetico.registry.ItemSource;
import dev.henny.cosmetico.state.Avatar;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class LifecycleTest {
    private final TestEngine t = new TestEngine();
    private final List<String> calls = new ArrayList<>();

    LifecycleTest() {
        t.engine.resources(new BundleResources() {
            @Override
            public void prepare(Bundle b) {
                calls.add("prepare " + b.id);
            }

            @Override
            public void evict(Bundle b) {
                calls.add("evict " + b.id);
            }

            @Override
            public void prefetchSounds(Bundle b) {
                calls.add("sounds " + b.id);
            }
        });
    }

    @Test
    void replacingABundleFreesTheOldOneOnce() {
        Bundle v1 = Fixtures.parse(Fixtures.cosmetic("t:a", "hat"));
        Bundle v2 = Fixtures.parse(Fixtures.cosmetic("t:a", "hat"));
        Item first = Item.of(v1, ItemSource.REMOTE, 0, null, null, 1);
        t.engine.items.put(first);
        t.engine.items.put(first);
        t.engine.items.put(Item.of(v2, ItemSource.REMOTE, 0, null, null, 2));
        t.engine.items.remove("t:a", ItemSource.REMOTE);
        assertEquals(List.of("evict t:a", "evict t:a"), calls);
    }

    @Test
    void hotSwappedEmoteRestartsWithTheNewBundle() {
        t.add(Fixtures.parse(Fixtures.emote("t:e")));
        Avatar a = t.engine.avatars.get(UUID.randomUUID());
        t.engine.emotes.play(a, "t:e");
        t.time.advance(0.5);
        Bundle v2 = Fixtures.parse(Fixtures.emote("t:e"));
        t.engine.items.put(Item.of(v2, ItemSource.DEV, 0, null, null, 3));
        assertSame(v2, a.emote.bundle);
        assertEquals(0.5, a.emote.start, 1e-9);
    }
}
