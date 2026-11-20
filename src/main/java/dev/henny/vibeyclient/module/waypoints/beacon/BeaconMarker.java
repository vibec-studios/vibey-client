package dev.henny.vibeyclient.module.waypoints.beacon;

import dev.henny.r3.math.Argb;
import dev.henny.r3.render.Light;
import dev.henny.r3.render.RenderMode;
import dev.henny.r3.render.WorldCanvas;
import dev.henny.vibeyclient.module.waypoints.core.WaypointKind;

final class BeaconMarker {
    static final int SKULL = 0x2620;
    static final float HALF_H = 0.6f;
    static final float HALF_W = 0.44f;
    static final float OUTLINE = 0.09f;
    static final float HOVER = 1.3f;

    private BeaconMarker() {}

    static float draw(WorldCanvas c, BeaconEffect b, float size, float alpha) {
        float k = size * (b.hovered() ? HOVER : 1f);
        float h = HALF_H * k, w = HALF_W * k, o = OUTLINE * k;
        int rgb = 0xFF000000 | b.waypoint().color();
        c.beginQuads(RenderMode.SEE_THROUGH, null);
        diamond(c, 0, w + o, h + o * 1.4f, Argb.withAlpha(0xFF000000, 0.7f * alpha));
        diamond(c, 0, w, h, Argb.multiplyAlpha(rgb, alpha));
        diamond(c, h * 0.28f, w * 0.42f, h * 0.36f, Argb.withAlpha(0xFFFFFFFF, 0.45f * alpha));
        if (b.navTarget()) ring(c, w + o * 3, h + o * 3.6f, o, Argb.withAlpha(0xFFFFFFFF, 0.8f * alpha));
        c.endQuads();
        if (b.waypoint().kind() == WaypointKind.DEATH) skull(c, k, alpha);
        return top(size, b.hovered());
    }

    static float top(float size, boolean hover) {
        float k = size * (hover ? HOVER : 1f);
        return HALF_H * k + OUTLINE * k * 1.4f;
    }

    private static void diamond(WorldCanvas c, float cy, float w, float h, int argb) {
        quad(c, 0, cy + h, w, cy, 0, cy - h, -w, cy, argb);
    }

    private static void ring(WorldCanvas c, float w, float h, float t, int argb) {
        quad(c, 0, h, w, 0, w - t, 0, 0, h - t * 1.4f, argb);
        quad(c, w, 0, 0, -h, 0, -h + t * 1.4f, w - t, 0, argb);
        quad(c, 0, -h, -w, 0, -w + t, 0, 0, -h + t * 1.4f, argb);
        quad(c, -w, 0, 0, h, 0, h - t * 1.4f, -w + t, 0, argb);
    }

    private static void quad(WorldCanvas c, float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, int argb) {
        int l = Light.FULL_BRIGHT;
        c.vertex(x0, y0, 0, 0, 0, argb, l);
        c.vertex(x1, y1, 0, 1, 0, argb, l);
        c.vertex(x2, y2, 0, 1, 1, argb, l);
        c.vertex(x3, y3, 0, 0, 1, argb, l);
    }

    private static void skull(WorldCanvas c, float k, float alpha) {
        float px = k / 9f * 1.25f;
        c.push();
        c.scale(px, -px, px);
        float adv = c.textMetrics().advance(SKULL);
        c.glyph(SKULL, -adv / 2f, -4f, Argb.withAlpha(0xFF101014, alpha), Light.FULL_BRIGHT, true);
        c.pop();
    }
}
