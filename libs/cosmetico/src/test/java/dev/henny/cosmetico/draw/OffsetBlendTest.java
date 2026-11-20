package dev.henny.cosmetico.draw;

import com.google.gson.JsonObject;
import dev.henny.cosmetico.fixtures.Fixtures;
import dev.henny.cosmetico.fixtures.TestEngine;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Slot;
import dev.henny.cosmetico.skeleton.Mix;
import dev.henny.cosmetico.state.Avatar;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OffsetBlendTest {
    private final TestEngine t = new TestEngine();
    private final Avatar a = new Avatar(UUID.randomUUID(), false);
    private final OffsetBlend blend = new OffsetBlend(t.engine.shown);
    private Bundle wings;

    OffsetBlendTest() {
        JsonObject w = Fixtures.cosmetic("t:wings", "wings");
        w.getAsJsonObject("cosmetic").add("offsetWhen", Fixtures.json("{\"r\":[{\"slot\":\"back\",\"offset\":[0,0,4]}]}").get("r"));
        wings = Fixtures.parse(w);
        JsonObject back = Fixtures.cosmetic("t:bag", "back");
        back.getAsJsonObject("cosmetic").add("hideWithArmor", Fixtures.json("{\"a\":[\"chest\"]}").get("a"));
        t.add(Fixtures.parse(back));
        a.loadout.base[Slot.BACK.ordinal()] = "t:bag";
    }

    private Mix at(double now) {
        Mix m = new Mix().clear();
        blend.apply(a, wings, Slot.WINGS, now, m);
        return m;
    }

    @Test
    void firstFrameSnapsInAndLaterFramesEase() {
        assertEquals(4f, at(1.0).rootOffset[2], 1e-6f);
        a.loadout.base[Slot.BACK.ordinal()] = null;
        Mix m = at(1.05);
        assertTrue(m.rootOffset[2] > 0f && m.rootOffset[2] < 4f, "" + m.rootOffset[2]);
        assertFalse(at(3.0).hasRootOffset);
    }

    @Test
    void armorHiddenItemsCountAsNotWorn() {
        a.frame.chestArmor = true;
        assertFalse(at(1.0).hasRootOffset);
    }
}
