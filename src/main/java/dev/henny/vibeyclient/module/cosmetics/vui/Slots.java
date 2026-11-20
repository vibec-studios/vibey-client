package dev.henny.vibeyclient.module.cosmetics.vui;

import dev.henny.cosmetico.model.Slot;
import dev.henny.cosmetico.registry.Item;

import java.util.List;

final class Slots {
    record Def(String id, Slot slot, String icon) {
        boolean emotes() {
            return slot == null;
        }

        boolean holds(Item item) {
            if (item.isEmote()) return emotes();
            return slot != null && slot == item.slot();
        }
    }

    static final List<Def> ALL = List.of(
            new Def("cape", Slot.CAPE, "cape"), new Def("wings", Slot.WINGS, "wings"), new Def("aura", Slot.AURA, "aura"),
            new Def("pet", Slot.PET, "paw"), new Def("shield", Slot.SHIELD, "shield"), new Def("hat", Slot.HAT, "hat"),
            new Def("back", Slot.BACK, "bag"), new Def("emotes", null, "wave"));

    private Slots() {}

    static Def of(String id) {
        for (Def d : ALL) if (d.id().equals(id)) return d;
        return ALL.get(0);
    }

    static Def of(Slot slot) {
        for (Def d : ALL) if (d.slot() == slot) return d;
        return ALL.get(0);
    }
}
