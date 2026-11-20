package dev.henny.vibeyclient.module.visuals.effects.core;

import java.util.Locale;

public enum Preset {
    VANILLA, CLEAN, PVP, FANCY;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Preset of(String id) {
        for (Preset p : values()) if (p.id().equals(id)) return p;
        return null;
    }
}
