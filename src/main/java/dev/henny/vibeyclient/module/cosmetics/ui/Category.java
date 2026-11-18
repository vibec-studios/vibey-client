package dev.henny.vibeyclient.module.cosmetics.ui;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.avatar.AvatarItem;
import dev.henny.vibeyclient.avatar.Slot;

/** The wardrobe's left column: one entry per cosmetic slot (SPEC §5 order) plus emotes. */
enum Category {
    CAPE(Slot.CAPE), WINGS(Slot.WINGS), AURA(Slot.AURA), PET(Slot.PET), SHIELD(Slot.SHIELD), HAT(Slot.HAT),
    BACK(Slot.BACK), EMOTES(null);

    static final Category[] ALL = values();

    /** Null for {@link #EMOTES}. */
    final Slot slot;
    private final String key;

    Category(Slot slot) {
        this.slot = slot;
        this.key = "wardrobe.category." + name().toLowerCase(java.util.Locale.ROOT);
    }

    String label() {
        return Lang.tr(key);
    }

    boolean emotes() {
        return slot == null;
    }

    static Category of(AvatarItem item) {
        if (item.isEmote()) return EMOTES;
        return item.slot() == null ? null : of(item.slot());
    }

    static Category of(Slot slot) {
        for (Category c : ALL) if (c.slot == slot) return c;
        return CAPE;
    }
}
