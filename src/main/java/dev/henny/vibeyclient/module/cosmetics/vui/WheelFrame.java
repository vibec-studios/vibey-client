package dev.henny.vibeyclient.module.cosmetics.vui;

public record WheelFrame(float tile, float centreX, float centreY, float centreSize, float ringX, float ringY,
                         float ringSize, float hintY) {
    static WheelFrame of(WheelGeometry g, float hintY) {
        float ring = 2 * g.radius() + g.tile() + 8;
        return new WheelFrame(g.tile(), g.centreX(), g.centreY(), g.centreSize(), Math.round(g.cx() - ring / 2),
                Math.round(g.cy() - ring / 2), ring, hintY);
    }
}
