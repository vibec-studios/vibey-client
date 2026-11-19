package dev.henny.r3.math;

import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class RectHit {
    private final Vector3d point = new Vector3d();
    private double distance;
    private double localX;
    private double localY;
    private double u;
    private double v;
    private boolean front;

    public void set(Vector3dc point, double distance, double localX, double localY, double u, double v, boolean front) {
        this.point.set(point);
        this.distance = distance;
        this.localX = localX;
        this.localY = localY;
        this.u = u;
        this.v = v;
        this.front = front;
    }

    public RectHit set(RectHit other) {
        set(other.point, other.distance, other.localX, other.localY, other.u, other.v, other.front);
        return this;
    }

    public Vector3dc point() {
        return point;
    }

    public double distance() {
        return distance;
    }

    public double localX() {
        return localX;
    }

    public double localY() {
        return localY;
    }

    public double u() {
        return u;
    }

    public double v() {
        return v;
    }

    public boolean front() {
        return front;
    }

    public boolean inside() {
        return u >= 0 && u <= 1 && v >= 0 && v <= 1;
    }

    public float pixelX(int pixelWidth) {
        return (float) (u * pixelWidth);
    }

    public float pixelY(int pixelHeight) {
        return (float) (v * pixelHeight);
    }
}
