package dev.henny.vibeyclient.avatar;

import java.util.Locale;

/** Cosmetic slots (SPEC §5), in wardrobe order. */
public enum Slot {
    CAPE, WINGS, AURA, PET, SHIELD, HAT, BACK;

    private static final Slot[] VALUES = values();

    /** "cape", "wings", ... as in the .vibey format and the backend. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Null for unknown ids. */
    public static Slot of(String id) {
        if (id == null) return null;
        for (Slot s : VALUES) if (s.id().equals(id)) return s;
        return null;
    }

    public static Slot[] all() {
        return VALUES;
    }
}
