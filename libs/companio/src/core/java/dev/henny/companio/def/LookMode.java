package dev.henny.companio.def;

import java.util.Locale;

public enum LookMode {
    PLAYER, MOVE, SKY, AROUND, NONE;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static LookMode of(String id) {
        for (LookMode m : values()) if (m.id().equals(id)) return m;
        return null;
    }
}
