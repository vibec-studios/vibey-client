package dev.henny.r3.interact;

import dev.henny.r3.math.OrientedRect;
import dev.henny.r3.math.Ray;
import dev.henny.r3.math.RectHit;

public final class EdgeDrag {
    private final OrientedRect start = new OrientedRect();
    private final RectHit hit = new RectHit();
    private Interactable target;
    private ResizeHandle handle = ResizeHandle.NONE;
    private PoseSnapshot before;
    private double grabDx;
    private double grabDy;

    public void begin(Interactable target, ResizeHandle handle, RectHit at) {
        this.target = target;
        this.handle = handle;
        this.before = PoseSnapshot.capture(target);
        start.set(target.transform());
        grabDx = at.localX() - handle.sx() * start.halfWidth();
        grabDy = at.localY() - handle.sy() * start.halfHeight();
    }

    public void follow(Ray ray) {
        if (target == null || !start.planeHit(ray, hit)) return;
        EdgeResize.apply(target.transform(), start, handle, hit.localX() - grabDx, hit.localY() - grabDy, target.rules());
    }

    public void end() {
        target = null;
        before = null;
        handle = ResizeHandle.NONE;
    }

    public Interactable target() {
        return target;
    }

    public ResizeHandle handle() {
        return handle;
    }

    public PoseSnapshot before() {
        return before;
    }
}
