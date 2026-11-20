package dev.henny.cosmetico.registry;

import dev.henny.cosmetico.fixtures.Fixtures;
import dev.henny.cosmetico.host.Log;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Slot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ItemRegistryTest {
    private final ItemRegistry reg = new ItemRegistry(() -> Log.NONE);

    private static Item item(String id, String slot, ItemSource src, int sort) {
        Bundle b = Fixtures.parse(Fixtures.cosmetic(id, slot));
        return Item.of(b, src, sort, null, null, 1);
    }

    @Test
    void higherPriorityWinsById() {
        reg.put(item("t:a", "hat", ItemSource.BUILTIN, 0));
        reg.put(item("t:a", "hat", ItemSource.REMOTE, 0));
        assertEquals(ItemSource.REMOTE, reg.get("t:a").source());
        reg.put(item("t:a", "hat", ItemSource.DEV, 0));
        assertEquals(ItemSource.DEV, reg.get("t:a").source());
        reg.put(item("t:a", "hat", new ItemSource("othermod", 500), 0));
        assertEquals(ItemSource.DEV, reg.get("t:a").source());
        reg.remove("t:a", ItemSource.DEV);
        assertEquals("othermod", reg.get("t:a").source().id());
        assertEquals(1, reg.all().size());
    }

    @Test
    void orderIsSlotThenSortThenIdEmotesLast() {
        reg.put(Item.of(Fixtures.parse(Fixtures.emote("t:e")), ItemSource.BUILTIN, 0, null, null, 1));
        reg.put(item("t:z", "cape", ItemSource.BUILTIN, 5));
        reg.put(item("t:b", "hat", ItemSource.BUILTIN, 1));
        reg.put(item("t:a", "hat", ItemSource.BUILTIN, 1));
        reg.put(item("t:y", "cape", ItemSource.BUILTIN, 1));
        List<String> ids = new ArrayList<>();
        for (Item i : reg.all()) ids.add(i.id());
        assertEquals(List.of("t:y", "t:z", "t:a", "t:b", "t:e"), ids);
        assertEquals(2, reg.bySlot(Slot.HAT).size());
        assertEquals(1, reg.emotes().size());
    }

    @Test
    void versionAndListeners() throws Exception {
        List<String> events = new ArrayList<>();
        reg.listen(() -> events.add("changed"));
        AutoCloseable c = reg.listenItems(new ItemListener() {
            @Override
            public void replaced(Item old, Item now) {
                events.add("replaced " + (old == null ? "-" : old.id()) + " " + now.id());
            }

            @Override
            public void removed(Item old) {
                events.add("removed " + old.id());
            }
        });
        long v = reg.version();
        reg.put(item("t:a", "hat", ItemSource.BUILTIN, 0));
        reg.putAll(ItemSource.BUILTIN, Map.of("t:b", item("t:b", "hat", ItemSource.BUILTIN, 0)), true);
        assertNull(reg.remove("t:nope", ItemSource.BUILTIN));
        c.close();
        reg.remove("t:b", ItemSource.BUILTIN);
        assertEquals(v + 3, reg.version());
        assertEquals(List.of("changed", "replaced - t:a", "changed", "replaced - t:b", "removed t:a", "changed"), events);
    }

    @Test
    void perSourceLookup() {
        Item builtin = item("t:a", "hat", ItemSource.BUILTIN, 0);
        reg.put(builtin);
        reg.put(item("t:a", "hat", ItemSource.DEV, 0));
        assertSame(builtin, reg.get("t:a", ItemSource.BUILTIN));
        assertNull(reg.get("t:a", ItemSource.REMOTE));
        assertEquals(reg.nextRevision() + 1, reg.nextRevision());
    }
}
