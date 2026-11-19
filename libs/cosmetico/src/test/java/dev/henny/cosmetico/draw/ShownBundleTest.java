package dev.henny.cosmetico.draw;

import dev.henny.cosmetico.fixtures.Fixtures;
import dev.henny.cosmetico.fixtures.TestEngine;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Slot;
import dev.henny.cosmetico.state.Avatar;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ShownBundleTest {
    private final TestEngine t = new TestEngine();
    private final Bundle hat = Fixtures.parse(Fixtures.cosmetic("t:hat", "hat"));

    @Test
    void slotTogglesAndServerRulesHideWorldAvatarsButNotPreviews() {
        t.add(hat);
        Avatar world = new Avatar(UUID.randomUUID(), false), preview = new Avatar(null, true);
        world.loadout.base[Slot.HAT.ordinal()] = "t:hat";
        preview.loadout.base[Slot.HAT.ordinal()] = "t:hat";
        assertSame(hat, t.engine.shown.bundle(world, Slot.HAT));
        t.engine.config.slotShown[Slot.HAT.ordinal()] = false;
        assertNull(t.engine.shown.bundle(world, Slot.HAT));
        t.engine.config.slotShown[Slot.HAT.ordinal()] = true;
        t.engine.config.slotServerOff[Slot.HAT.ordinal()] = true;
        assertNull(t.engine.shown.bundle(world, Slot.HAT));
        assertSame(hat, t.engine.shown.bundle(preview, Slot.HAT));
    }

    @Test
    void wrongSlotEmotesAndMissingItemsShowNothing() {
        t.add(hat);
        t.add(Fixtures.parse(Fixtures.emote("t:e")));
        Avatar a = new Avatar(UUID.randomUUID(), false);
        a.loadout.base[Slot.BACK.ordinal()] = "t:hat";
        a.loadout.base[Slot.CAPE.ordinal()] = "t:e";
        a.loadout.base[Slot.PET.ordinal()] = "t:gone";
        assertNull(t.engine.shown.bundle(a, Slot.BACK));
        assertNull(t.engine.shown.bundle(a, Slot.CAPE));
        assertNull(t.engine.shown.bundle(a, Slot.PET));
    }

    @Test
    void hiddenVisibilityRule() {
        assertTrue(SlotVisibility.hidden(t.engine.config, false, true, false));
        assertFalse(SlotVisibility.hidden(t.engine.config, true, false, false));
        t.engine.config.showOwn = false;
        assertTrue(SlotVisibility.hidden(t.engine.config, true, false, false));
        assertFalse(SlotVisibility.hidden(t.engine.config, false, false, false));
    }
}
