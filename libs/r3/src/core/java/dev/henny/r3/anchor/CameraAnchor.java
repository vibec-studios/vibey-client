package dev.henny.r3.anchor;

import dev.henny.r3.math.Angles;
import dev.henny.r3.object.Transform;
import dev.henny.r3.view.CameraState;
import org.joml.Vector3d;

public final class CameraAnchor implements Anchor {
    private final double distance;
    private final float yawOffset;
    private final float pitchOffset;
    private final float smoothing;
    private final Vector3d direction = new Vector3d();
    private float yaw = Float.NaN;
    private float pitch;

    public CameraAnchor(double distance, float yawOffset, float pitchOffset, float smoothing) {
        this.distance = distance;
        this.yawOffset = yawOffset;
        this.pitchOffset = pitchOffset;
        this.smoothing = Math.max(0f, smoothing);
    }

    public static CameraAnchor glued(double distance) {
        return new CameraAnchor(distance, 0f, 0f, 0f);
    }

    public double distance() {
        return distance;
    }

    public float yawOffset() {
        return yawOffset;
    }

    public float pitchOffset() {
        return pitchOffset;
    }

    public float smoothing() {
        return smoothing;
    }

    @Override
    public AnchorKind kind() {
        return AnchorKind.CAMERA;
    }

    @Override
    public boolean resolve(AnchorContext context, Transform target) {
        CameraState camera = context.camera();
        if (camera == null) return false;
        follow(camera.yaw() + yawOffset, camera.pitch() + pitchOffset, context.dt());
        Angles.direction(yaw, pitch, direction);
        target.position().set(camera.position()).fma(distance, direction);
        Facing.CAMERA.apply(target, camera, 0f);
        return true;
    }

    private void follow(float targetYaw, float targetPitch, float dt) {
        if (Float.isNaN(yaw) || smoothing <= 0f) {
            yaw = targetYaw;
            pitch = targetPitch;
            return;
        }
        float k = 1f - (float) Math.exp(-dt * smoothing);
        yaw = Angles.wrapDegrees(yaw + Angles.wrapDegrees(targetYaw - yaw) * k);
        pitch += (targetPitch - pitch) * k;
    }

    public void reset() {
        yaw = Float.NaN;
    }

    @Override
    public Anchor copy() {
        return new CameraAnchor(distance, yawOffset, pitchOffset, smoothing);
    }

    @Override
    public boolean followsSomething() {
        return true;
    }
}
