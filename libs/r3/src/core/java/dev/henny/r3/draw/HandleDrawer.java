package dev.henny.r3.draw;

import dev.henny.r3.interact.HoverState;
import dev.henny.r3.interact.Interactable;
import dev.henny.r3.interact.ResizeHandle;
import dev.henny.r3.object.Transform;
import dev.henny.r3.render.Light;
import dev.henny.r3.render.Quads;
import dev.henny.r3.render.RenderMode;
import dev.henny.r3.render.WorldCanvas;
import org.joml.Vector3f;

public final class HandleDrawer {
    public static final int HANDLE_COLOR = 0xD0FFFFFF;
    public static final int HELD_COLOR = 0xFF4C8BFF;
    public static final double FRONT_OFFSET = 0.006;

    private final Basis basis = new Basis();
    private final Vector3f edgeCenter = new Vector3f();

    public void drawHover(WorldCanvas canvas, HoverState hover, double distance) {
        Interactable t = hover.target();
        if (t == null || !hover.zone().onBorder()) return;
        ResizeHandle h = hover.handle();
        float thick = (float) Math.max(0.015, distance * 0.004);
        basis.set(t.transform(), canvas.cameraPosition(), FRONT_OFFSET);
        canvas.beginQuads(RenderMode.EMISSIVE, null);
        if (h.sx() != 0) verticalEdge(canvas, t.transform(), h.sx(), thick, HANDLE_COLOR);
        if (h.sy() != 0) horizontalEdge(canvas, t.transform(), h.sy(), thick, HANDLE_COLOR);
        canvas.endQuads();
    }

    public void drawOutline(WorldCanvas canvas, Interactable target, double distance) {
        float thick = (float) Math.max(0.01, distance * 0.0025);
        basis.set(target.transform(), canvas.cameraPosition(), FRONT_OFFSET);
        canvas.beginQuads(RenderMode.EMISSIVE, null);
        verticalEdge(canvas, target.transform(), 1, thick, HELD_COLOR);
        verticalEdge(canvas, target.transform(), -1, thick, HELD_COLOR);
        horizontalEdge(canvas, target.transform(), 1, thick, HELD_COLOR);
        horizontalEdge(canvas, target.transform(), -1, thick, HELD_COLOR);
        canvas.endQuads();
    }

    private void verticalEdge(WorldCanvas canvas, Transform t, int side, float thick, int argb) {
        float dx = side * t.worldWidth() / 2f;
        edgeCenter.set(basis.right).mul(dx).add(basis.center);
        Quads.quad(canvas, edgeCenter, basis.right, basis.up, thick, t.worldHeight() / 2f + thick, argb, Light.FULL_BRIGHT);
    }

    private void horizontalEdge(WorldCanvas canvas, Transform t, int side, float thick, int argb) {
        float dy = side * t.worldHeight() / 2f;
        edgeCenter.set(basis.up).mul(dy).add(basis.center);
        Quads.quad(canvas, edgeCenter, basis.right, basis.up, t.worldWidth() / 2f + thick, thick, argb, Light.FULL_BRIGHT);
    }
}
