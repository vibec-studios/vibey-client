package dev.henny.vibeyclient.module.clips.shots.core;

import java.util.Comparator;

public enum ShotSort {
    NEWEST, OLDEST, NAME;

    public Comparator<Shot> comparator() {
        Comparator<Shot> byName = Comparator.comparing(Shot::name, String.CASE_INSENSITIVE_ORDER);
        return switch (this) {
            case NEWEST -> Comparator.comparingLong(Shot::modifiedMs).reversed().thenComparing(byName);
            case OLDEST -> Comparator.comparingLong(Shot::modifiedMs).thenComparing(byName);
            case NAME -> byName;
        };
    }

    public ShotSort next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static ShotSort parse(String s) {
        for (ShotSort v : values()) if (v.name().equalsIgnoreCase(s)) return v;
        return NEWEST;
    }
}
