package dev.henny.cosmetico.preview;

import dev.henny.cosmetico.fixtures.Fixtures;
import dev.henny.cosmetico.fixtures.TestEngine;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Slot;
import dev.henny.cosmetico.sim.PetGeometry;
import dev.henny.cosmetico.state.Avatar;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreviewStateTest {
    private final TestEngine t = new TestEngine();
    private final PreviewState view = t.engine.preview();

    @Test
    void dragRotatesOnlyWhenItStartedInside() {
        view.frame(0, 0, 100, 200, 0, 1, null);
        assertFalse(view.mouse(MouseAction.PRESS, 150, 10));
        assertFalse(view.mouse(MouseAction.DRAG, 160, 10));
        assertTrue(view.mouse(MouseAction.PRESS, 50, 10));
        assertTrue(view.mouse(MouseAction.DRAG, 60, 10));
        assertEquals(-9f, view.yaw(), 1e-5f);
        assertTrue(view.mouse(MouseAction.RELEASE, 60, 10));
        assertFalse(view.mouse(MouseAction.RELEASE, 60, 10));
    }

    @Test
    void autoRotateTurnsEighteenDegreesPerSecond() {
        view.setAutoRotate(true);
        view.frame(0, 0, 100, 200, 0, 1.0, null);
        view.frame(0, 0, 100, 200, 0, 1.05, null);
        assertEquals(0.9f, view.yaw(), 1e-4f);
    }

    @Test
    void mirrorsTheOwnLoadoutWithTryOnOverrides() {
        Bundle pet = Fixtures.parse(Fixtures.cosmetic("t:pet", "pet"));
        t.add(pet);
        Avatar self = new Avatar(UUID.randomUUID(), false);
        self.loadout.base[Slot.HAT.ordinal()] = "t:hat";
        view.override(Slot.PET, "t:pet");
        float size = view.frame(0, 0, 310, 1000, 0, 1, self);
        assertEquals("t:hat", view.avatar.shown(Slot.HAT));
        assertEquals(310 / (float) Math.max(3.1, 2 * (PetGeometry.side(pet) + PetGeometry.halfWidth(pet)) + 0.3), size, 1e-4f);
        assertEquals(180f, view.avatar.frame.bodyYaw);
        view.clearOverrides();
        assertEquals(Math.min(1000 / 2.3f, 310 / 1.6f), view.frame(0, 0, 310, 1000, 0, 1, self), 1e-3f);
    }

    @Test
    void previewEmotesMakeItemsHappy() {
        t.add(Fixtures.parse(Fixtures.emote("t:e")));
        view.playEmote("t:e");
        assertEquals("t:e", view.emote());
        view.frame(0, 0, 100, 200, 0, 0.1, null);
        assertEquals(1f, view.avatar.motion.happyTarget);
        view.stopEmote();
        assertNull(view.emote());
    }
}
