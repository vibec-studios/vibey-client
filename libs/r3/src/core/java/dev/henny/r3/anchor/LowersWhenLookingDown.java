package dev.henny.r3.anchor;

import dev.henny.r3.math.Angles;
import dev.henny.r3.object.Transform;
import dev.henny.r3.view.CameraState;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class LowersWhenLookingDown implements Anchor {
    public static final float PITCH_SMOOTHING = 4.5f;
    public static final float FLOOR_DEGREES = 2f;

    private final Anchor inner;
    private final Vector3d direction = new Vector3d();
    private float pitch = Float.NaN;

    public LowersWhenLookingDown(Anchor inner) {
        this.inner = inner;
    }

    public Anchor inner() {
        return inner;
    }

    public void reset() {
        pitch = Float.NaN;
    }

    public static float lowered(float elevation, float smoothedPitch) {
        return Math.max(Math.min(FLOOR_DEGREES, elevation), elevation - Math.max(0f, smoothedPitch));
    }

    @Override
    public AnchorKind kind() {
        return inner.kind();
    }

    @Override
    public boolean resolve(AnchorContext context, Transform target) {
        if (!inner.resolve(context, target)) return false;
        CameraState camera = context.camera();
        if (camera == null) return true;
        smooth(camera.pitch(), context.dt());
        Vector3dc eye = camera.position();
        Vector3d p = target.position();
        double dx = p.x - eye.x(), dy = p.y - eye.y(), dz = p.z - eye.z();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        float elevation = -Angles.pitchOf(dx, dy, dz);
        Angles.direction(Angles.yawOf(dx, dz), -lowered(elevation, pitch), direction);
        p.set(eye).fma(distance, direction);
        Facing.CAMERA.apply(target, camera, 0f);
        return true;
    }

    private void smooth(float cameraPitch, float dt) {
        if (Float.isNaN(pitch)) {
            pitch = cameraPitch;
            return;
        }
        pitch += (cameraPitch - pitch) * (1f - (float) Math.exp(-dt * PITCH_SMOOTHING));
    }

    @Override
    public Anchor copy() {
        return new LowersWhenLookingDown(inner.copy());
    }

    @Override
    public boolean followsSomething() {
        return inner.followsSomething();
    }
}
