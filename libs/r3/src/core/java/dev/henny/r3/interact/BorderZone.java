package dev.henny.r3.interact;

public final class BorderZone {
    private BorderZone() {}

    public static double width(double distance, double halfWidth, double halfHeight, InteractionConfig c) {
        double w = Math.max(c.borderMin, distance * c.borderPerBlock);
        return Math.min(w, Math.min(halfWidth, halfHeight) * 2 * c.borderMaxFraction);
    }

    public static double outside(double border, InteractionConfig c) {
        return border * (1 - c.borderInsideFraction);
    }

    public static double inside(double border, InteractionConfig c) {
        return border * c.borderInsideFraction;
    }

    public static ResizeHandle handleAt(double localX, double localY, double halfWidth, double halfHeight, double inside) {
        int sx = localX > halfWidth - inside ? 1 : localX < -halfWidth + inside ? -1 : 0;
        int sy = localY > halfHeight - inside ? 1 : localY < -halfHeight + inside ? -1 : 0;
        return ResizeHandle.of(sx, sy);
    }
}
