package dev.henny.r3.object;

import dev.henny.r3.math.Axes;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;

public final class Transform {
    private final Vector3d position = new Vector3d();
    private final Quaternionf orientation = new Quaternionf();
    private float width = 1f;
    private float height = 1f;
    private float scale = 1f;

    public Vector3d position() {
        return position;
    }

    public Quaternionf orientation() {
        return orientation;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public float scale() {
        return scale;
    }

    public float worldWidth() {
        return width * scale;
    }

    public float worldHeight() {
        return height * scale;
    }

    public float aspect() {
        return height <= 0f ? 1f : width / height;
    }

    public Transform position(double x, double y, double z) {
        position.set(x, y, z);
        return this;
    }

    public Transform orientation(Quaternionfc q) {
        orientation.set(q);
        return this;
    }

    public Transform size(float width, float height) {
        this.width = Math.max(1e-4f, width);
        this.height = Math.max(1e-4f, height);
        return this;
    }

    public Transform scale(float scale) {
        this.scale = Math.max(1e-4f, scale);
        return this;
    }

    public Transform worldSize(float worldWidth, float worldHeight) {
        return size(worldWidth / scale, worldHeight / scale);
    }

    public Vector3d right(Vector3d out) {
        return Axes.right(orientation, out);
    }

    public Vector3d up(Vector3d out) {
        return Axes.up(orientation, out);
    }

    public Vector3d normal(Vector3d out) {
        return Axes.normal(orientation, out);
    }

    public double boundingRadius() {
        double hw = worldWidth() / 2.0;
        double hh = worldHeight() / 2.0;
        return Math.sqrt(hw * hw + hh * hh);
    }

    public Transform set(Transform other) {
        position.set(other.position);
        orientation.set(other.orientation);
        width = other.width;
        height = other.height;
        scale = other.scale;
        return this;
    }

    public Transform copy() {
        return new Transform().set(this);
    }

    public boolean sameAs(Transform other, double epsilon) {
        return position.distance(other.position) <= epsilon
                && Math.abs(width - other.width) <= epsilon
                && Math.abs(height - other.height) <= epsilon
                && Math.abs(scale - other.scale) <= epsilon
                && Math.abs(Math.abs(orientation.dot(other.orientation)) - 1f) <= epsilon;
    }
}
