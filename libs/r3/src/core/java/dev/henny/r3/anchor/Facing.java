package dev.henny.r3.anchor;

import dev.henny.r3.math.Axes;
import dev.henny.r3.object.Transform;
import dev.henny.r3.view.CameraState;
import org.joml.Vector3dc;

public enum Facing {
    KEEP,
    CAMERA,
    CAMERA_YAW,
    ENTITY_YAW;

    public void apply(Transform target, CameraState camera, float entityYaw) {
        Vector3dc p = target.position();
        double nx = camera.position().x() - p.x();
        double ny = camera.position().y() - p.y();
        double nz = camera.position().z() - p.z();
        switch (this) {
            case CAMERA -> Axes.facing(nx, ny, nz, target.orientation());
            case CAMERA_YAW -> Axes.facing(nx, 0, nz, target.orientation());
            case ENTITY_YAW -> {
                double yaw = Math.toRadians(entityYaw);
                Axes.facing(-Math.sin(yaw), 0, Math.cos(yaw), target.orientation());
            }
            default -> {
            }
        }
    }
}
