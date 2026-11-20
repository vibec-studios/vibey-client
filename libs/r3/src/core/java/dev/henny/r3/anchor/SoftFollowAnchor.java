package dev.henny.r3.anchor;

import dev.henny.r3.math.Angles;
import dev.henny.r3.object.Transform;
import dev.henny.r3.view.CameraState;
import org.joml.Vector3d;

public final class SoftFollowAnchor implements Anchor {
    public static final float SOFT_RECENTER_DEGREES = 30f;
    public static final float SOFT_RATE = 2.6f;
    public static final float FOLLOW_RATE = 4.5f;
    public static final float SETTLE_DEGREES = 2.5f;
    public static final float NEVER = 360f;

    private final double distance;
    private final float elevation;
    private final float recenterDegrees;
    private final float rate;
    private final float startYaw;
    private final Vector3d direction = new Vector3d();
    private float yaw = Float.NaN;
    private boolean recentering;

    public SoftFollowAnchor(double distance, float elevation, float recenterDegrees, float rate, float startYaw) {
        this.distance = distance;
        this.elevation = elevation;
        this.recenterDegrees = recenterDegrees;
        this.rate = Math.max(0f, rate);
        this.startYaw = startYaw;
    }

    public static SoftFollowAnchor soft(double distance, float elevation) {
        return new SoftFollowAnchor(distance, elevation, SOFT_RECENTER_DEGREES, SOFT_RATE, Float.NaN);
    }

    public static SoftFollowAnchor follow(double distance, float elevation) {
        return new SoftFollowAnchor(distance, elevation, 0f, FOLLOW_RATE, Float.NaN);
    }

    public static SoftFollowAnchor fixed(double distance, float yaw, float elevation) {
        return new SoftFollowAnchor(distance, elevation, NEVER, 0f, yaw);
    }

    public double distance() {
        return distance;
    }

    public float elevation() {
        return elevation;
    }

    public float recenterDegrees() {
        return recenterDegrees;
    }

    public float rate() {
        return rate;
    }

    public float startYaw() {
        return startYaw;
    }

    public float yaw() {
        return yaw;
    }

    public boolean recentering() {
        return recentering;
    }

    public void reset() {
        yaw = Float.NaN;
        recentering = false;
    }

    @Override
    public AnchorKind kind() {
        return AnchorKind.SOFT_FOLLOW;
    }

    @Override
    public boolean resolve(AnchorContext context, Transform target) {
        CameraState camera = context.camera();
        if (camera == null) return false;
        step(camera.yaw(), context.dt());
        Angles.direction(yaw, -elevation, direction);
        target.position().set(camera.position()).fma(distance, direction);
        Facing.CAMERA.apply(target, camera, 0f);
        return true;
    }

    public void step(float cameraYaw, float dt) {
        if (Float.isNaN(yaw)) {
            yaw = Float.isNaN(startYaw) ? cameraYaw : startYaw;
            return;
        }
        float d = Angles.wrapDegrees(cameraYaw - yaw);
        if (Math.abs(d) > recenterDegrees) recentering = true;
        if (!recentering) return;
        yaw = Angles.wrapDegrees(yaw + d * (1f - (float) Math.exp(-dt * rate)));
        if (Math.abs(Angles.wrapDegrees(cameraYaw - yaw)) < SETTLE_DEGREES) recentering = false;
    }

    @Override
    public Anchor copy() {
        return new SoftFollowAnchor(distance, elevation, recenterDegrees, rate, startYaw);
    }

    @Override
    public boolean followsSomething() {
        return true;
    }
}
