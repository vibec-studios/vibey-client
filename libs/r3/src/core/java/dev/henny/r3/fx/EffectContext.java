package dev.henny.r3.fx;

import dev.henny.r3.math.Axes;
import dev.henny.r3.view.CameraState;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;

public final class EffectContext {
    private final Vector3d origin = new Vector3d();
    private final Quaternionf orientation = new Quaternionf();
    private final Vector3d right = new Vector3d(1, 0, 0);
    private final Vector3d up = new Vector3d(0, 1, 0);
    private final Vector3d normal = new Vector3d(0, 0, 1);
    private final Vector3d camera = new Vector3d();
    private final Vector3f cameraRight = new Vector3f(1, 0, 0);
    private final Vector3f cameraUp = new Vector3f(0, 1, 0);
    private float lod = 1f;
    private float alpha = 1f;
    private int light;
    private EffectBudget budget = EffectBudget.UNLIMITED;

    public EffectContext origin(Vector3dc position, Quaternionfc orientation) {
        origin.set(position);
        this.orientation.set(orientation);
        Axes.right(orientation, right);
        Axes.up(orientation, up);
        Axes.normal(orientation, normal);
        return this;
    }

    public EffectContext camera(CameraState cam) {
        camera.set(cam.position());
        cameraRight.set((float) cam.right().x(), (float) cam.right().y(), (float) cam.right().z());
        cameraUp.set((float) cam.up().x(), (float) cam.up().y(), (float) cam.up().z());
        return this;
    }

    public EffectContext detail(float lod, float alpha, int light, EffectBudget budget) {
        this.lod = lod;
        this.alpha = alpha;
        this.light = light;
        this.budget = budget;
        return this;
    }

    public Vector3dc origin() {
        return origin;
    }

    public Quaternionfc orientation() {
        return orientation;
    }

    public Vector3dc right() {
        return right;
    }

    public Vector3dc up() {
        return up;
    }

    public Vector3dc normal() {
        return normal;
    }

    public Vector3dc cameraPosition() {
        return camera;
    }

    public Vector3f cameraRight() {
        return cameraRight;
    }

    public Vector3f cameraUp() {
        return cameraUp;
    }

    public float lod() {
        return lod;
    }

    public float alpha() {
        return alpha;
    }

    public int light() {
        return light;
    }

    public EffectBudget budget() {
        return budget;
    }
}
