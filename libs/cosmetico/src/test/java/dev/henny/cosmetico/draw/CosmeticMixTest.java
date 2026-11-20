package dev.henny.cosmetico.draw;

import dev.henny.cosmetico.fixtures.Fixtures;
import dev.henny.cosmetico.fixtures.TestEngine;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Slot;
import dev.henny.cosmetico.skeleton.Mix;
import dev.henny.cosmetico.state.Avatar;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CosmeticMixTest {
    private static final String VIS = "{\"length\":1,\"loop\":true,\"events\":[{\"t\":0,\"type\":\"visibility\",\"bone\":\"b\",\"visible\":false}]}";

    @Test
    void idleKeepsVisibilityUnlessAnotherStateClearlyDominates() {
        TestEngine t = new TestEngine();
        var o = Fixtures.cosmetic("t:pet", "pet");
        Fixtures.animation(o, "idle", VIS);
        Fixtures.animation(o, "walk", VIS);
        Bundle b = Fixtures.parse(o);
        Avatar a = new Avatar(UUID.randomUUID(), false);
        CosmeticMix mixer = t.engine.cosmeticMix();
        a.pet.move = 0.4f;
        Mix m = mixer.of(a, b, Slot.PET, 1.0);
        assertEquals("idle", m.visibility.name);
        a.pet.move = 0.9f;
        assertEquals("walk", mixer.of(a, b, Slot.PET, 1.0).visibility.name);
    }

    @Test
    void emptyOptionalStatesCountAsMissing() {
        TestEngine t = new TestEngine();
        var o = Fixtures.cosmetic("t:hat", "hat");
        Fixtures.animation(o, "idle", "{\"length\":1}");
        Fixtures.animation(o, "happy", "{\"length\":1}");
        Bundle b = Fixtures.parse(o);
        Avatar a = new Avatar(UUID.randomUUID(), false);
        a.motion.happy = 1f;
        Mix m = t.engine.cosmeticMix().of(a, b, Slot.HAT, 1.0);
        assertEquals(1, m.count);
        assertEquals("idle", m.anims[0].name);
    }
}
