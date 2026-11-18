package dev.henny.vibeyclient.avatar;

import java.util.Locale;

/** Purely cosmetic label (SPEC: no prices anywhere). */
public enum Rarity {
    COMMON(0xFFA1A1AA), RARE(0xFF3FE0C5), EPIC(0xFF8B6CFF), LEGENDARY(0xFFFFB547);

    /** ARGB label colour (Vibe palette). */
    public final int color;

    Rarity(int color) {
        this.color = color;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Rarity of(String id) {
        if (id != null) for (Rarity r : values()) if (r.id().equals(id)) return r;
        return COMMON;
    }
}
