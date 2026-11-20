package dev.henny.companio.world;

import java.util.Locale;

public enum Command {
    FOLLOW, STAY, SIT;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Command of(String id) {
        for (Command c : values()) if (c.id().equals(id)) return c;
        return FOLLOW;
    }
}
