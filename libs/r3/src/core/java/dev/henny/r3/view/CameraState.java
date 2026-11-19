package dev.henny.r3.view;

import dev.henny.r3.math.Angles;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class CameraState {
    private final Vector3d position = new Vector3d();
    private final Vector3d look = new Vector3d(0, 0, 1);
    private final Vector3d right = new Vector3d(-1, 0, 0);
    private final Vector3d up = new Vector3d(0, 1, 0);
    private float yaw;
    private float pitch;

    public CameraState set(double x, double y, double z, float yawDegrees, float pitchDegrees) {
        position.set(x, y, z);
        return rotation(yawDegrees, pitchDegrees);
    }

    public CameraState position(double x, double y, double z) {
        position.set(x, y, z);
        return this;
    }

    public CameraState rotation(float yawDegrees, float pitchDegrees) {
        yaw = yawDegrees;
        pitch = Math.max(-90f, Math.min(90f, pitchDegrees));
        Angles.direction(yaw, pitch, look);
        double yr = Math.toRadians(yaw);
        right.set(-Math.cos(yr), 0, -Math.sin(yr));
        up.set(right).cross(look).normalize();
        return this;
    }

    public CameraState lookAlong(double dx, double dy, double dz) {
        return rotation(Angles.yawOf(dx, dz), Angles.pitchOf(dx, dy, dz));
    }

    public CameraState set(CameraState other) {
        position.set(other.position);
        return rotation(other.yaw, other.pitch);
    }

    public Vector3dc position() {
        return position;
    }

    public Vector3dc look() {
        return look;
    }

    public Vector3dc right() {
        return right;
    }

    public Vector3dc up() {
        return up;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public double distanceTo(Vector3dc p) {
        return position.distance(p);
    }
}
