package dev.henny.r3.render;

public enum RenderMode {
    CUTOUT,
    TRANSLUCENT,
    ADDITIVE,
    EMISSIVE;

    public boolean sortsBackToFront() {
        return this == TRANSLUCENT;
    }

    public boolean ignoresLight() {
        return this == ADDITIVE || this == EMISSIVE;
    }
}
