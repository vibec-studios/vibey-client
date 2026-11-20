package dev.henny.vibeyclient.module.waypoints.beacon;

import dev.henny.r3.fx.EffectContext;
import dev.henny.r3.math.Argb;
import dev.henny.r3.object.Transform;
import dev.henny.r3.render.Light;
import dev.henny.r3.render.Quads;
import dev.henny.r3.render.RenderMode;
import dev.henny.r3.render.TextMetrics;
import dev.henny.r3.render.WorldCanvas;
import dev.henny.vibeyclient.module.waypoints.core.WaypointKind;
import org.joml.Vector3dc;

final class BeaconDraw {
    static final int DIAMOND = 0x25C6;
    static final int SKULL = 0x2620;
    private static final float BEAM_HEIGHT = 160f;
    private static final float TEXT = 0.55f;
    private static final float GLYPH = 2.2f;

    private BeaconDraw() {}

    static void draw(WorldCanvas c, EffectContext ctx, BeaconEffect b, Transform t, boolean beams) {
        Vector3dc o = ctx.origin();
        Vector3dc cam = c.cameraPosition();
        float ox = (float) (o.x() - cam.x()), oy = (float) (o.y() - cam.y()), oz = (float) (o.z() - cam.z());
        float size = t.worldWidth();
        float near = (float) Math.max(0, Math.min(1, (b.distance() - 2.5) / 2.5));
        float alpha = ctx.alpha() * near * (b.foreign() ? 0.6f : 1f);
        if (alpha <= 0.01f) return;
        if (beams && !b.foreign()) beam(c, b, ox, oy, oz, size, alpha);
        marker(c, ctx, b, ox, oy, oz, size, alpha);
    }

    private static void beam(WorldCanvas c, BeaconEffect b, float ox, float oy, float oz, float size, float alpha) {
        int rgb = 0xFF000000 | b.waypoint().color();
        float pulse = b.navTarget() ? 0.75f + 0.25f * (float) Math.sin(b.time() * 6f) : 0.6f;
        float base = oy - (float) BeaconAnchor.LIFT * (b.anchor().clamped() ? size : 1f);
        float top = oy + BEAM_HEIGHT;
        c.beginQuads(RenderMode.ADDITIVE, null);
        Quads.ribbon(c, ox, base, oz, ox, top, oz, size * 0.2f, size * 0.2f,
                Argb.withAlpha(rgb, 0.3f * pulse * alpha), Argb.withAlpha(rgb, 0f), Light.FULL_BRIGHT);
        Quads.ribbon(c, ox, base, oz, ox, top, oz, size * 0.05f, size * 0.05f,
                Argb.withAlpha(rgb, 0.8f * pulse * alpha), Argb.withAlpha(rgb, 0f), Light.FULL_BRIGHT);
        c.endQuads();
    }

    private static void marker(WorldCanvas c, EffectContext ctx, BeaconEffect b, float ox, float oy, float oz,
                               float size, float alpha) {
        boolean hover = b.hovered();
        TextMetrics m = c.textMetrics();
        c.push();
        c.translate(ox, oy, oz);
        c.rotate(ctx.orientation());
        float px = size / 9f;
        c.scale(px, -px, px);
        int cp = b.waypoint().kind() == WaypointKind.DEATH ? SKULL : DIAMOND;
        float k = hover ? GLYPH * 1.3f : GLYPH;
        c.push();
        c.scale(k, k, k);
        float adv = m.advance(cp);
        int rgb = 0xFF000000 | b.waypoint().color();
        c.glyph(cp, -adv / 2f, -4.5f, Argb.multiplyAlpha(rgb, alpha), Light.FULL_BRIGHT, true);
        c.pop();
        c.scale(TEXT, TEXT, TEXT);
        BeaconLabels l = b.labels();
        float top = -14f / TEXT;
        line(c, m, l.title(), top, Argb.multiplyAlpha(0xFFFFFFFF, alpha));
        if (hover) {
            line(c, m, l.coords(), top - 10f, Argb.multiplyAlpha(0xFFCFCFE0, alpha));
            if (!l.portal().isEmpty()) line(c, m, l.portal(), top - 20f, Argb.multiplyAlpha(0xFF3FE0C5, alpha));
        }
        c.pop();
    }

    private static void line(WorldCanvas c, TextMetrics m, String s, float y, int argb) {
        if (s.isEmpty()) return;
        c.text(s, -m.width(s) / 2f, y, argb, Light.FULL_BRIGHT, true);
    }
}
