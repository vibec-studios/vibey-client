package dev.henny.vibeyclient.module.inventory.core;

public enum Section {
    CONTAINER, MAIN, HOTBAR, NONE;

    public boolean player() {
        return this == MAIN || this == HOTBAR;
    }

    public boolean opposite(Section other, boolean hasContainer) {
        if (this == NONE || other == NONE) return false;
        if (hasContainer) return player() != other.player();
        return player() && other.player() && this != other;
    }
}
