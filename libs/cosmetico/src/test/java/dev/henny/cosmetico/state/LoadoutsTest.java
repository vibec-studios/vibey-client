package dev.henny.cosmetico.state;

import dev.henny.cosmetico.model.Slot;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LoadoutsTest {
    private final AvatarRegistry avatars = new AvatarRegistry();
    private final AvatarLoadouts loadouts = new AvatarLoadouts(avatars);
    private final UUID p = UUID.randomUUID();

    @Test
    void setReplacesEverySlot() {
        loadouts.set(p, Map.of(Slot.HAT, "t:hat", Slot.CAPE, "t:cape"));
        loadouts.set(p, Map.of(Slot.HAT, "t:hat2"));
        assertEquals(Map.of(Slot.HAT, "t:hat2"), loadouts.get(p));
    }

    @Test
    void idsFromTheWireIgnoreUnknownSlotsAndEmptyValues() {
        Map<String, String> wire = new HashMap<>();
        wire.put("tail", "t:x");
        wire.put("wings", "");
        wire.put("pet", "t:pet");
        wire.put("aura", null);
        loadouts.setIds(p, wire);
        assertEquals(Map.of(Slot.PET, "t:pet"), loadouts.get(p));
    }

    @Test
    void overridesWinUntilCleared() {
        loadouts.set(p, Map.of(Slot.HAT, "t:hat"));
        loadouts.override(p, Slot.HAT, null);
        loadouts.override(p, Slot.BACK, "t:bag");
        Avatar a = avatars.get(p);
        assertNull(a.shown(Slot.HAT));
        assertEquals("t:bag", a.shown(Slot.BACK));
        loadouts.set(p, Map.of(Slot.HAT, "t:other"));
        assertNull(a.shown(Slot.HAT));
        loadouts.clearOverrides(p);
        assertEquals("t:other", a.shown(Slot.HAT));
        loadouts.clearAll();
        assertNull(a.shown(Slot.HAT));
    }
}
