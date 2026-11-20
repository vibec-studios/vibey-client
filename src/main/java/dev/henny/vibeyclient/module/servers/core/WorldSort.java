package dev.henny.vibeyclient.module.servers.core;

public enum WorldSort {
    CUSTOM, LAST_PLAYED, NAME;

    public WorldSort next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
