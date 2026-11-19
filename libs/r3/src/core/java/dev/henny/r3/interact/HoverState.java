package dev.henny.r3.interact;

import dev.henny.r3.math.RectHit;

public final class HoverState {
    private final RectHit hit = new RectHit();
    private Interactable target;
    private HoverZone zone = HoverZone.NONE;
    private ResizeHandle handle = ResizeHandle.NONE;
    private String controlId;

    public void clear() {
        target = null;
        zone = HoverZone.NONE;
        handle = ResizeHandle.NONE;
        controlId = null;
    }

    public void set(Interactable target, HoverZone zone, ResizeHandle handle, String controlId, RectHit hit) {
        this.target = target;
        this.zone = zone;
        this.handle = handle;
        this.controlId = controlId;
        this.hit.set(hit);
    }

    public Interactable target() {
        return target;
    }

    public HoverZone zone() {
        return zone;
    }

    public ResizeHandle handle() {
        return handle;
    }

    public String controlId() {
        return controlId;
    }

    public RectHit hit() {
        return hit;
    }

    public float pixelX() {
        return target == null ? 0f : hit.pixelX(target.pixelWidth());
    }

    public float pixelY() {
        return target == null ? 0f : hit.pixelY(target.pixelHeight());
    }
}
