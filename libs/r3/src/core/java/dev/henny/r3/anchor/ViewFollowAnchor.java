package dev.henny.r3.anchor;

import dev.henny.r3.math.Angles;
import dev.henny.r3.math.SmoothDamp;
import dev.henny.r3.object.Transform;
import dev.henny.r3.view.CameraState;
import org.joml.Vector3d;

public final class ViewFollowAnchor implements Anchor {
    public static final float COMFORT_DEGREES = 20f;
    public static final float DELAY_SECONDS = 0.35f;
    public static final float FAR_DEGREES = 55f;
    public static final float GLIDE_TIME = 0.5f;
    public static final float GLIDE_MAX_SPEED = 170f;
    public static final float ABOVE_CROSSHAIR = 9f;
    public static final float MIN_ELEVATION = 9f;
    public static final float MAX_ELEVATION = 58f;

    private final double distance;
    private final SmoothDamp yawSpring = new SmoothDamp(GLIDE_TIME, GLIDE_MAX_SPEED);
    private final SmoothDamp elevationSpring = new SmoothDamp(GLIDE_TIME, GLIDE_MAX_SPEED);
    private final Vector3d direction = new Vector3d();
    private float yaw = Float.NaN;
    private float elevation;
    private boolean gliding;
    private float outsideFor;

    public ViewFollowAnchor(double distance) {
        this.distance = distance;
    }

    public double distance() {
        return distance;
    }

    public float yaw() {
        return yaw;
    }

    public float elevation() {
        return elevation;
    }

    public boolean gliding() {
        return gliding;
    }

    public void reset() {
        yaw = Float.NaN;
    }

    @Override
    public AnchorKind kind() {
        return AnchorKind.VIEW_FOLLOW;
    }

    @Override
    public boolean resolve(AnchorContext context, Transform target) {
        CameraState camera = context.camera();
        if (camera == null) return false;
        step(camera.yaw(), camera.pitch(), context.dt());
        Angles.direction(yaw, -elevation, direction);
        target.position().set(camera.position()).fma(distance, direction);
        Facing.CAMERA.apply(target, camera, 0f);
        return true;
    }

    public void step(float cameraYaw, float cameraPitch, float dt) {
        float targetElevation = Math.max(MIN_ELEVATION, Math.min(MAX_ELEVATION, -cameraPitch + ABOVE_CROSSHAIR));
        if (Float.isNaN(yaw)) {
            restart(cameraYaw, targetElevation);
            return;
        }
        float dYaw = Angles.wrapDegrees(cameraYaw - yaw);
        float horizontal = dYaw * (float) Math.cos(Math.toRadians(elevation));
        float dElevation = targetElevation - elevation;
        float away = (float) Math.sqrt(horizontal * horizontal + dElevation * dElevation);
        if (!gliding) startGlideIfNeeded(away, dt);
        if (gliding) glide(dYaw, targetElevation, away, dt);
    }

    private void restart(float cameraYaw, float targetElevation) {
        yaw = cameraYaw;
        elevation = targetElevation;
        gliding = false;
        outsideFor = 0f;
        yawSpring.reset();
        elevationSpring.reset();
    }

    private void startGlideIfNeeded(float away, float dt) {
        if (away <= COMFORT_DEGREES) {
            outsideFor = 0f;
            return;
        }
        outsideFor += dt;
        if (outsideFor > DELAY_SECONDS || away > FAR_DEGREES) gliding = true;
    }

    private void glide(float dYaw, float targetElevation, float away, float dt) {
        yaw = Angles.wrapDegrees(yawSpring.step(yaw, yaw + dYaw, dt));
        elevation = elevationSpring.step(elevation, targetElevation, dt);
        float speed = Math.abs(yawSpring.velocity()) + Math.abs(elevationSpring.velocity());
        if (away < 1.2f && speed < 3f) {
            gliding = false;
            outsideFor = 0f;
            yawSpring.reset();
            elevationSpring.reset();
        }
    }

    @Override
    public Anchor copy() {
        return new ViewFollowAnchor(distance);
    }

    @Override
    public boolean followsSomething() {
        return true;
    }
}
