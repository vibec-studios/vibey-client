package dev.henny.r3.math;

public final class SmoothDamp {
    private final float smoothTime;
    private final float maxSpeed;
    private float velocity;

    public SmoothDamp(float smoothTime, float maxSpeed) {
        this.smoothTime = smoothTime;
        this.maxSpeed = maxSpeed;
    }

    public float velocity() {
        return velocity;
    }

    public void reset() {
        velocity = 0f;
    }

    public float step(float current, float target, float dt) {
        float omega = 2f / smoothTime;
        float x = omega * dt;
        float exp = 1f / (1f + x + 0.48f * x * x + 0.235f * x * x * x);
        float maxChange = maxSpeed * smoothTime;
        float change = Math.max(-maxChange, Math.min(maxChange, current - target));
        float clampedTarget = current - change;
        float temp = (velocity + omega * change) * dt;
        velocity = (velocity - omega * temp) * exp;
        float out = clampedTarget + (change + temp) * exp;
        if ((target - current > 0f) == (out > target)) {
            out = target;
            velocity = 0f;
        }
        return out;
    }
}
