package dev.henny.vibeyclient.module.waypoints.beacon;

import dev.henny.r3.fx.EffectContext;
import dev.henny.r3.object.Transform;
import dev.henny.r3.render.WorldCanvas;
import org.joml.Vector3dc;

final class BeaconDraw {
    private BeaconDraw() {}

    static void draw(WorldCanvas c, EffectContext ctx, BeaconEffect b, Transform t, BeaconState state) {
        Vector3dc o = ctx.origin();
        Vector3dc cam = c.cameraPosition();
        float ox = (float) (o.x() - cam.x()), oy = (float) (o.y() - cam.y()), oz = (float) (o.z() - cam.z());
        float size = t.worldWidth();
        float near = (float) Math.max(0, Math.min(1, (b.distance() - 2.5) / 2.5));
        float alpha = ctx.alpha() * near * (b.foreign() ? 0.6f : 1f);
        if (alpha <= 0.01f) return;
        if (state.beams() && !b.foreign()) BeaconBeam.draw(c, b, ox, oy, oz, size, alpha, state.beamsThrough());
        float distance = (float) Math.sqrt(ox * ox + oy * oy + oz * oz);
        c.push();
        c.translate(ox, oy, oz);
        c.rotate(ctx.orientation());
        float top = BeaconMarker.draw(c, b, size, alpha);
        BeaconLabel.draw(c, b, size, top, distance, state.pixelAngle(), alpha);
        c.pop();
    }
}
