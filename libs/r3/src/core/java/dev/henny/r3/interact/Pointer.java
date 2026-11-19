package dev.henny.r3.interact;

import dev.henny.r3.math.Ray;
import dev.henny.r3.view.CameraState;
import dev.henny.r3.view.PointerRays;

public final class Pointer {
    public static final double DEFAULT_REACH = 64;

    private final Ray ray = new Ray();
    private final CameraState camera = new CameraState();
    private double maxDistance = DEFAULT_REACH;
    private boolean hasCamera;

    public Pointer fromCrosshair(CameraState camera, double maxDistance) {
        this.camera.set(camera);
        PointerRays.fromCrosshair(camera, ray);
        this.maxDistance = maxDistance;
        hasCamera = true;
        return this;
    }

    public Pointer fromRay(Ray ray, CameraState camera, double maxDistance) {
        this.ray.set(ray);
        hasCamera = camera != null;
        if (hasCamera) this.camera.set(camera);
        this.maxDistance = maxDistance;
        return this;
    }

    public Pointer fromScreenPoint(double x, double y) {
        PointerRays.fromScreenPoint(x, y, ray);
        hasCamera = false;
        maxDistance = DEFAULT_REACH;
        return this;
    }

    public Pointer set(Pointer other) {
        ray.set(other.ray);
        camera.set(other.camera);
        maxDistance = other.maxDistance;
        hasCamera = other.hasCamera;
        return this;
    }

    public Ray ray() {
        return ray;
    }

    public CameraState camera() {
        return hasCamera ? camera : null;
    }

    public float viewerYaw() {
        return hasCamera ? camera.yaw() : 0f;
    }

    public double maxDistance() {
        return maxDistance;
    }

    public Pointer maxDistance(double maxDistance) {
        this.maxDistance = maxDistance;
        return this;
    }
}
