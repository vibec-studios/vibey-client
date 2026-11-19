package dev.henny.r3.object;

public final class Visibility {
    private double maxDistance = 128;
    private double fadeDistance = 8;
    private boolean hidden;
    private float opacity = 1f;

    public double maxDistance() {
        return maxDistance;
    }

    public double fadeDistance() {
        return fadeDistance;
    }

    public boolean hidden() {
        return hidden;
    }

    public Visibility maxDistance(double maxDistance) {
        this.maxDistance = Math.max(0, maxDistance);
        return this;
    }

    public Visibility fadeDistance(double fadeDistance) {
        this.fadeDistance = Math.max(0, fadeDistance);
        return this;
    }

    public float opacity() {
        return opacity;
    }

    public Visibility opacity(float opacity) {
        this.opacity = Math.max(0f, Math.min(1f, opacity));
        return this;
    }

    public Visibility hidden(boolean hidden) {
        this.hidden = hidden;
        return this;
    }

    public float alphaAt(double distance) {
        if (hidden || distance > maxDistance) return 0f;
        double start = maxDistance - fadeDistance;
        if (fadeDistance <= 0 || distance <= start) return opacity;
        return opacity * (float) Math.max(0, Math.min(1, (maxDistance - distance) / fadeDistance));
    }
}
