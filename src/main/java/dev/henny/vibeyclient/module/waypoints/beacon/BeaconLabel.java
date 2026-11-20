package dev.henny.vibeyclient.module.waypoints.beacon;

import dev.henny.r3.math.Argb;
import dev.henny.r3.render.Light;
import dev.henny.r3.render.TextMetrics;
import dev.henny.r3.render.WorldCanvas;

final class BeaconLabel {
    static final float GUI = 0.75f;
    static final float GUI_HOVER = 0.85f;
    static final float LINE = 10f;
    static final float CAP = 0.06f;
    static final float GAP = 0.12f;

    private BeaconLabel() {}

    static void draw(WorldCanvas c, BeaconEffect b, float size, float markerTop, float distance, double pixelAngle, float alpha) {
        boolean hover = b.hovered();
        float px = scale(distance, pixelAngle, size, hover);
        BeaconLabels l = b.labels();
        TextMetrics m = c.textMetrics();
        c.push();
        c.translate(0, markerTop + GAP * size, 0);
        c.scale(px, -px, px);
        float y = -LINE - b.labelLevel() * LINE;
        line(c, m, l.title(), y, Argb.multiplyAlpha(0xFFFFFFFF, alpha));
        if (hover) {
            line(c, m, l.coords(), y - LINE, Argb.multiplyAlpha(0xFFCFCFE0, alpha));
            if (!l.portal().isEmpty()) line(c, m, l.portal(), y - 2 * LINE, Argb.multiplyAlpha(0xFF3FE0C5, alpha));
        }
        c.pop();
    }

    static float scale(float distance, double pixelAngle, float size, boolean hover) {
        float screen = (float) ((hover ? GUI_HOVER : GUI) * pixelAngle * distance);
        return Math.min(screen, size * CAP * (hover ? 1.3f : 1f));
    }

    private static void line(WorldCanvas c, TextMetrics m, String s, float y, int argb) {
        if (s.isEmpty()) return;
        c.text(s, -m.width(s) / 2f, y, argb, Light.FULL_BRIGHT, true);
    }
}
