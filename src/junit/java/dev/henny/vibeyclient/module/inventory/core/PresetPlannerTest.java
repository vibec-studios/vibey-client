package dev.henny.vibeyclient.module.inventory.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresetPlannerTest {
    private static ItemRef r(String id) {
        return new ItemRef("minecraft:" + id, null);
    }

    private static List<ItemRef> inventory() {
        List<ItemRef> inv = new ArrayList<>();
        for (int i = 0; i < PresetPlanner.INVENTORY; i++) inv.add(ItemRef.EMPTY);
        return inv;
    }

    private static int apply(List<ItemRef> inv, HotbarPreset p) {
        int swaps = 0;
        PresetPlanner.Swap s;
        while ((s = PresetPlanner.next(inv, p)) != null && swaps < 20) {
            ItemRef a = inv.get(s.from());
            inv.set(s.from(), inv.get(s.hotbar()));
            inv.set(s.hotbar(), a);
            swaps++;
        }
        return swaps;
    }

    @Test
    void swapsItemsFromTheInventoryIntoPlace() {
        List<ItemRef> inv = inventory();
        inv.set(0, r("dirt"));
        inv.set(12, r("diamond_sword"));
        inv.set(20, r("bow"));
        HotbarPreset p = new HotbarPreset(List.of(r("diamond_sword"), r("bow")));
        assertEquals(2, apply(inv, p));
        assertEquals(r("diamond_sword"), inv.get(0));
        assertEquals(r("bow"), inv.get(1));
        assertEquals(r("dirt"), inv.get(12));
        assertTrue(PresetPlanner.done(inv, p));
    }

    @Test
    void movesWithinTheHotbarAndKeepsSatisfiedSlots() {
        List<ItemRef> inv = inventory();
        inv.set(0, r("bow"));
        inv.set(1, r("sword"));
        HotbarPreset p = new HotbarPreset(List.of(r("sword"), r("bow")));
        assertEquals(1, apply(inv, p));
        assertEquals(r("sword"), inv.get(0));
        assertEquals(r("bow"), inv.get(1));
    }

    @Test
    void missingItemsAreSkipped() {
        List<ItemRef> inv = inventory();
        HotbarPreset p = new HotbarPreset(List.of(r("totem_of_undying")));
        assertNull(PresetPlanner.next(inv, p));
    }

    @Test
    void namedItemsArePreferred() {
        List<ItemRef> inv = inventory();
        inv.set(9, r("diamond_sword"));
        inv.set(10, new ItemRef("minecraft:diamond_sword", "Slayer"));
        HotbarPreset p = new HotbarPreset(List.of(new ItemRef("minecraft:diamond_sword", "Slayer")));
        assertEquals(new PresetPlanner.Swap(10, 0), PresetPlanner.next(inv, p));
    }

    @Test
    void presetSetCyclesAndResizes() {
        PresetSet set = new PresetSet(3);
        assertEquals(1, set.next());
        set.select(2);
        assertEquals(0, set.next());
        set.resize(10);
        assertEquals(PresetSet.MAX, set.size());
        set.resize(1);
        assertEquals(PresetSet.MIN, set.size());
        assertEquals(1, set.active());
    }

    @Test
    void jsonRoundTripDropsBadIds() {
        PresetSet set = new PresetSet(3);
        set.put(1, new HotbarPreset(List.of(r("bow"), new ItemRef("minecraft:stick", "Wand"))));
        set.select(1);
        JsonObject json = PresetJson.write(set);
        PresetSet back = new PresetSet(3);
        PresetJson.read(json, back);
        assertEquals(1, back.active());
        assertEquals(set.get(1), back.get(1));
        PresetSet bad = new PresetSet(2);
        PresetJson.read(JsonParser.parseString("{\"active\":9,\"presets\":[[{\"id\":\"Not Valid\"}]]}").getAsJsonObject(), bad);
        assertTrue(bad.get(0).blank());
        assertEquals(1, bad.active());
    }
}
