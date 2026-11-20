package dev.henny.vui.anim;

public final class Spring {
    private static final float REST_DISTANCE = 0.01f;
    private static final float REST_VELOCITY = 0.01f;
    private static final float MAX_STEP = 1f / 120f;

    private final float stiffness;
    private final float damping;
    private final float mass;
    private float value;
    private float velocity;
    private float target;

    public Spring(float value, float stiffness, float damping, float mass) {
        this.value = value;
        this.target = value;
        this.stiffness = stiffness;
        this.damping = damping;
        this.mass = mass;
    }

    public static Spring standard(float value) {
        return new Spring(value, 260f, 24f, 1f);
    }

    public static Spring bouncy(float value) {
        return new Spring(value, 300f, 12f, 1f);
    }

    public void target(float next) {
        target = next;
        if (!Animations.enabled()) snap(next);
    }

    public float target() {
        return target;
    }

    public void step(float dt) {
        if (!Animations.enabled()) {
            snap(target);
            return;
        }
        float remaining = dt * Animations.speed();
        while (remaining > 0f && !atRest()) {
            float h = Math.min(MAX_STEP, remaining);
            float force = -stiffness * (value - target) - damping * velocity;
            velocity += force / mass * h;
            value += velocity * h;
            remaining -= h;
        }
        if (atRest()) snap(target);
    }

    public boolean atRest() {
        return Math.abs(value - target) < REST_DISTANCE && Math.abs(velocity) < REST_VELOCITY;
    }

    public float value() {
        return value;
    }

    public float velocity() {
        return velocity;
    }

    public void snap(float v) {
        value = v;
        target = v;
        velocity = 0f;
    }
}
