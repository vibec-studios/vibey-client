package dev.henny.r3.interact;

public enum HoverZone {
    NONE,
    BODY,
    CONTROL,
    EDGE,
    CORNER;

    public boolean onContent() {
        return this == BODY || this == CONTROL;
    }

    public boolean onBorder() {
        return this == EDGE || this == CORNER;
    }
}
