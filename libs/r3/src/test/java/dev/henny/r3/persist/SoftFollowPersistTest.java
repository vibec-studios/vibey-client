package dev.henny.r3.persist;

import dev.henny.r3.anchor.SoftFollowAnchor;
import dev.henny.r3.interact.InteractionRules;
import dev.henny.r3.object.WorldObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoftFollowPersistTest {
    @Test
    void softFollowWithoutStartYawSavesAndLoads() {
        WorldObject o = WorldObject.create("music").anchoredTo(SoftFollowAnchor.soft(2.0, -10f));
        PlacementStore store = new PlacementStore();
        store.put("w", PlacementCodec.encode(o, null));
        String json = store.toJson();
        PlacementRecord back = PlacementStore.fromJson(json).list("w").get(0);
        WorldObject loaded = PlacementCodec.decode(back, InteractionRules.screen().limits());
        SoftFollowAnchor a = assertInstanceOf(SoftFollowAnchor.class, loaded.anchor());
        assertTrue(Float.isNaN(a.startYaw()));
        assertEquals(o.id(), loaded.id());
    }
}
