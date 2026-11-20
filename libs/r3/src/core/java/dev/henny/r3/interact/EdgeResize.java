package dev.henny.r3.interact;

import dev.henny.r3.math.OrientedRect;
import dev.henny.r3.object.Transform;

public final class EdgeResize {
    private static final ThreadLocal<OrientedRect> CURRENT = ThreadLocal.withInitial(OrientedRect::new);

    private EdgeResize() {}

    public static void apply(Transform t, OrientedRect start, ResizeHandle handle, double edgeX, double edgeY,
                             InteractionRules rules) {
        int sx = handle.sx();
        int sy = handle.sy();
        float w0 = (float) (2 * start.halfWidth());
        float h0 = (float) (2 * start.halfHeight());
        double fixedX = -sx * start.halfWidth();
        double fixedY = -sy * start.halfHeight();
        float w = sx != 0 ? (float) (sx * (edgeX - fixedX)) : w0;
        float h = sy != 0 ? (float) (sy * (edgeY - fixedY)) : h0;
        SizeLimits limits = rules.limits();
        if (rules.keepAspect()) {
            float factor = sx != 0 && sy != 0 ? Math.max(w / w0, h / h0) : sx != 0 ? w / w0 : h / h0;
            factor = limits.clampFactor(factor, w0, h0);
            w = w0 * factor;
            h = h0 * factor;
        } else {
            w = limits.clampWidth(w);
            h = limits.clampHeight(h);
        }
        double cx = sx != 0 ? fixedX + sx * w / 2.0 : 0;
        double cy = sy != 0 ? fixedY + sy * h / 2.0 : 0;
        t.position().set(start.center()).fma(cx, start.right()).fma(cy, start.up());
        t.worldSize(w, h);
    }

    public static boolean step(Transform t, ResizeHandle handle, float delta, InteractionRules rules) {
        if (handle == ResizeHandle.NONE) return false;
        OrientedRect rect = CURRENT.get().set(t);
        float w = t.worldWidth();
        float h = t.worldHeight();
        double edgeX = handle.sx() * (rect.halfWidth() + delta);
        double edgeY = handle.sy() * (rect.halfHeight() + delta);
        apply(t, rect, handle, edgeX, edgeY, rules);
        return Math.abs(t.worldWidth() - w) > 1e-6f || Math.abs(t.worldHeight() - h) > 1e-6f;
    }
}
