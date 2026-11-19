package dev.henny.r3.anchor;

import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class EntityPose {
    private final Vector3d position = new Vector3d();
    private float height = 1.8f;
    private float bodyYaw;
    private boolean sneaking;

    public EntityPose set(double x, double y, double z, float height, float bodyYaw, boolean sneaking) {
        position.set(x, y, z);
        this.height = height;
        this.bodyYaw = bodyYaw;
        this.sneaking = sneaking;
        return this;
    }

    public Vector3dc position() {
        return position;
    }

    public float height() {
        return height;
    }

    public float bodyYaw() {
        return bodyYaw;
    }

    public boolean sneaking() {
        return sneaking;
    }
}
