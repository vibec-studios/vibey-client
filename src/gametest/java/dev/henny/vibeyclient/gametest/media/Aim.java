package dev.henny.vibeyclient.gametest.media;

import dev.henny.r3.object.Transform;
import dev.henny.r3.object.VideoSurface;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.vui.VuiPanelContent;
import dev.henny.vui.dom.Element;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import org.joml.Vector3d;

final class Aim {
    private Aim() {}

    static Vector3d point(WorldObject o, double u, double v) {
        Transform t = o.transform();
        Vector3d right = t.orientation().transform(new Vector3d(1, 0, 0));
        Vector3d up = t.orientation().transform(new Vector3d(0, 1, 0));
        return new Vector3d(t.position()).add(right.mul((u - 0.5) * t.worldWidth())).add(up.mul((0.5 - v) * t.worldHeight()));
    }

    static VuiPanelContent document(WorldObject o) {
        if (o.content() instanceof VuiPanelContent p) return p;
        if (o.content() instanceof VideoSurface v && v.overlay() != null) return (VuiPanelContent) v.overlay().content();
        throw new AssertionError("no vui document on " + o.kind());
    }

    static Vector3d control(WorldObject o, String id) {
        Element e = document(o).document().find(id);
        if (e == null) throw new AssertionError("no control " + id + " on " + o.kind());
        double u = (e.box().x + e.box().width / 2.0) / o.pixelWidth();
        double v = (e.box().y + e.box().height / 2.0) / o.pixelHeight();
        return point(o, u, v);
    }

    static Vector3d rightEdge(WorldObject o, double outside) {
        Transform t = o.transform();
        Vector3d right = t.orientation().transform(new Vector3d(1, 0, 0));
        return new Vector3d(t.position()).add(right.mul(t.worldWidth() / 2.0 + outside));
    }

    static Vector3d leftEdge(WorldObject o) {
        Transform t = o.transform();
        Vector3d right = t.orientation().transform(new Vector3d(1, 0, 0));
        return new Vector3d(t.position()).sub(right.mul(t.worldWidth() / 2.0));
    }

    static void at(ClientGameTestContext ctx, Vector3d target) {
        at(ctx, target, 3);
    }

    static void at(ClientGameTestContext ctx, Vector3d target, int ticks) {
        float[] r = ctx.computeOnClient(mc -> {
            var e = mc.player.getEyePosition();
            double dx = target.x - e.x, dy = target.y - e.y, dz = target.z - e.z;
            return new float[] {(float) Math.toDegrees(Math.atan2(-dx, dz)), (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)))};
        });
        ctx.runOnClient(mc -> {
            mc.player.setYRot(r[0]);
            mc.player.setXRot(r[1]);
            mc.player.yRotO = r[0];
            mc.player.xRotO = r[1];
            mc.player.setYHeadRot(r[0]);
        });
        Film.wait(ctx, ticks);
    }

    static void glide(ClientGameTestContext ctx, Vector3d from, Vector3d to, int steps) {
        for (int i = 1; i <= steps; i++) at(ctx, new Vector3d(from).lerp(to, i / (double) steps), 1);
    }
}
