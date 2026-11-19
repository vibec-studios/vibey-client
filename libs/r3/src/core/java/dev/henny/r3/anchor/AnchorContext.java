package dev.henny.r3.anchor;

import dev.henny.r3.view.CameraState;

public final class AnchorContext {
    private WorldQuery query;
    private CameraState camera;
    private float dt;

    public AnchorContext set(WorldQuery query, CameraState camera, float dt) {
        this.query = query;
        this.camera = camera;
        this.dt = dt;
        return this;
    }

    public WorldQuery query() {
        return query;
    }

    public CameraState camera() {
        return camera;
    }

    public float dt() {
        return dt;
    }
}
