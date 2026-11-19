package dev.henny.cosmetico.model;

import java.util.Locale;

public enum Rarity {
    COMMON, RARE, EPIC, LEGENDARY;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Rarity of(String id) {
        if (id != null) for (Rarity r : values()) if (r.id().equals(id)) return r;
        return COMMON;
    }
}
