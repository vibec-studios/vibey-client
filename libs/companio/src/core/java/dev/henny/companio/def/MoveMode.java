package dev.henny.companio.def;

import java.util.Locale;

public enum MoveMode {
    FOLLOW, NEAR, STAY, SIT, SLEEP, WANDER, SWIM, FLEE, HOP, CIRCLE, SHOULDER, HEAD, FLY, NONE;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static MoveMode of(String id) {
        for (MoveMode m : values()) if (m.id().equals(id)) return m;
        return null;
    }

    public boolean riding() {
        return this == SHOULDER || this == HEAD;
    }
}
