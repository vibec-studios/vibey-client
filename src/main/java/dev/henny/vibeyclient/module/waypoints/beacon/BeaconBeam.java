package dev.henny.vibeyclient.module.waypoints.beacon;

import dev.henny.r3.math.Argb;
import dev.henny.r3.render.Light;
import dev.henny.r3.render.Quads;
import dev.henny.r3.render.RenderMode;
import dev.henny.r3.render.WorldCanvas;

final class BeaconBeam {
    static final float HEIGHT = 160f;
    static final float THROUGH = 0.22f;

    private BeaconBeam() {}

    static void draw(WorldCanvas c, BeaconEffect b, float ox, float oy, float oz, float size, float alpha, boolean through) {
        int rgb = 0xFF000000 | b.waypoint().color();
        float pulse = b.navTarget() ? 0.75f + 0.25f * (float) Math.sin(b.time() * 6f) : 0.6f;
        float base = oy - (float) BeaconAnchor.LIFT * (b.anchor().clamped() ? size : 1f);
        float top = oy + HEIGHT;
        c.beginQuads(RenderMode.ADDITIVE, null);
        Quads.ribbon(c, ox, base, oz, ox, top, oz, size * 0.2f, size * 0.2f,
                Argb.withAlpha(rgb, 0.3f * pulse * alpha), Argb.withAlpha(rgb, 0f), Light.FULL_BRIGHT);
        Quads.ribbon(c, ox, base, oz, ox, top, oz, size * 0.05f, size * 0.05f,
                Argb.withAlpha(rgb, 0.8f * pulse * alpha), Argb.withAlpha(rgb, 0f), Light.FULL_BRIGHT);
        c.endQuads();
        if (!through) return;
        c.beginQuads(RenderMode.SEE_THROUGH, null);
        Quads.ribbon(c, ox, base, oz, ox, top, oz, size * 0.06f, size * 0.06f,
                Argb.withAlpha(rgb, THROUGH * pulse * alpha), Argb.withAlpha(rgb, 0f), Light.FULL_BRIGHT);
        c.endQuads();
    }
}
