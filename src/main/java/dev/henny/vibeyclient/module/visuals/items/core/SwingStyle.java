package dev.henny.vibeyclient.module.visuals.items.core;

public enum SwingStyle {
    VANILLA,
    CLASSIC,
    SMOOTH,
    PUNCH;

    public boolean ownCurve() {
        return this == SMOOTH || this == PUNCH;
    }

    public boolean keepsItemUp() {
        return this == CLASSIC;
    }
}
