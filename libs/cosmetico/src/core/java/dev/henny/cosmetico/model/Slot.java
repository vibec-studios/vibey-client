package dev.henny.cosmetico.model;

import java.util.Locale;

public enum Slot {
    CAPE, WINGS, AURA, PET, SHIELD, HAT, BACK;

    private static final Slot[] VALUES = values();

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Slot of(String id) {
        if (id == null) return null;
        for (Slot s : VALUES) if (s.id().equals(id)) return s;
        return null;
    }

    public static Slot[] all() {
        return VALUES;
    }

    public static int count() {
        return VALUES.length;
    }
}
