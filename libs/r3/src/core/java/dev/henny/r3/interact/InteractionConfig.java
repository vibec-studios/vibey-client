package dev.henny.r3.interact;

public final class InteractionConfig {
    public double borderPerBlock = 0.03;
    public double borderMin = 0.04;
    public double borderMaxFraction = 0.35;
    public double borderInsideFraction = 0.5;
    public float wheelStep = 0.25f;
    public float fineFactor = 0.25f;
    public float edgeStep = 0.125f;
    public double distanceStep = 0.5;
    public double minHoldDistance = 0.75;
    public double maxHoldDistance = 16;
    public double placeDistance = 3;
    public double snapReach = 1.5;
    public double maxReach = 24;
    public double faceOffset = 0.02;
    public double grid = 0.125;
    public float wheelCoalesceSeconds = 1f;
    public int undoDepth = 16;
    public float hintSeconds = 6f;

    public static InteractionConfig defaults() {
        return new InteractionConfig();
    }

    public static InteractionConfig hud() {
        InteractionConfig c = new InteractionConfig();
        c.borderPerBlock = 0;
        c.borderMin = 4;
        c.wheelStep = 8f;
        c.edgeStep = 4f;
        c.grid = 0;
        return c;
    }
}
