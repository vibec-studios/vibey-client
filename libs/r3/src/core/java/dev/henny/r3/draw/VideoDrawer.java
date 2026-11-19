package dev.henny.r3.draw;

import dev.henny.r3.math.Argb;
import dev.henny.r3.object.Transform;
import dev.henny.r3.object.VideoSurface;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.render.Light;
import dev.henny.r3.render.Quads;
import dev.henny.r3.render.RenderMode;
import dev.henny.r3.render.TextMetrics;
import dev.henny.r3.render.WorldCanvas;

import java.util.List;

public final class VideoDrawer {
    public static final double BACK_OFFSET = -0.012;
    public static final double PANEL_OFFSET = -0.004;
    public static final double TEXT_OFFSET = 0.002;

    private final Basis basis = new Basis();

    public void draw(WorldCanvas canvas, WorldObject object, VideoSurface surface, int light) {
        Transform t = object.transform();
        float alpha = object.state().alpha();
        float hw = t.worldWidth() / 2f;
        float hh = t.worldHeight() / 2f;
        basis.set(t, canvas.cameraPosition(), BACK_OFFSET);
        canvas.beginQuads(RenderMode.TRANSLUCENT, null);
        Quads.doubleSided(canvas, basis.center, basis.right, basis.up, hw + surface.bezel(), hh + surface.bezel(),
                Argb.multiplyAlpha(surface.bezelColor(), alpha), light);
        canvas.endQuads();
        if (surface.showsPicture()) {
            basis.set(t, canvas.cameraPosition(), 0);
            canvas.beginQuads(RenderMode.CUTOUT, surface.texture());
            Quads.quad(canvas, basis.center, basis.right, basis.up, hw, hh, surface.u0(), surface.v0(), surface.u1(), surface.v1(),
                    Argb.multiplyAlpha(0xFFFFFFFF, alpha), light);
            canvas.endQuads();
            surface.markDrawn();
            return;
        }
        basis.set(t, canvas.cameraPosition(), PANEL_OFFSET);
        canvas.beginQuads(RenderMode.TRANSLUCENT, null);
        Quads.quad(canvas, basis.center, basis.right, basis.up, hw, hh, Argb.multiplyAlpha(surface.panelColor(), alpha), light);
        canvas.endQuads();
        if (surface.status() != null && !surface.status().isEmpty()) drawStatus(canvas, object, surface, alpha);
    }

    private void drawStatus(WorldCanvas canvas, WorldObject object, VideoSurface surface, float alpha) {
        Transform t = object.transform();
        TextMetrics metrics = canvas.textMetrics();
        float scale = t.worldHeight() / (metrics.lineHeight() * 6.4f);
        List<String> lines = surface.statusWrap().wrap(surface.status(), metrics, t.worldWidth() * 0.9f / scale);
        for (int i = 0; i < 4 && lines.size() > 3; i++) {
            scale *= 0.8f;
            lines = surface.statusWrap().wrap(surface.status(), metrics, t.worldWidth() * 0.9f / scale);
        }
        basis.set(t, canvas.cameraPosition(), TEXT_OFFSET);
        canvas.push();
        canvas.translate(basis.center.x, basis.center.y, basis.center.z);
        canvas.rotate(t.orientation());
        canvas.scale(scale, -scale, scale);
        float top = -lines.size() * metrics.lineHeight() / 2f;
        int color = Argb.multiplyAlpha(surface.statusColor(), alpha);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            canvas.text(line, -metrics.width(line) / 2f, top + i * metrics.lineHeight(), color, Light.FULL_BRIGHT, false);
        }
        canvas.pop();
    }
}
