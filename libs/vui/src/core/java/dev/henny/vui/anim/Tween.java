package dev.henny.vui.anim;

public final class Tween {
    private float from;
    private float to;
    private float durationSeconds;
    private float delaySeconds;
    private float elapsed;
    private Easing easing = Easings.OUT;
    private boolean running;

    public Tween(float value) {
        this.from = value;
        this.to = value;
    }

    public Tween start(float from, float to, float durationSeconds, float delaySeconds, Easing easing) {
        this.from = from;
        this.to = to;
        this.durationSeconds = durationSeconds;
        this.delaySeconds = delaySeconds;
        this.easing = easing == null ? Easings.OUT : easing;
        this.elapsed = 0f;
        this.running = from != to && Animations.enabled() && durationSeconds > 0f;
        return this;
    }

    public Tween to(float target, float durationSeconds) {
        if (target == to) return this;
        return start(value(), target, durationSeconds, 0f, easing);
    }

    public void step(float dt) {
        if (!running) return;
        if (!Animations.enabled()) {
            running = false;
            return;
        }
        elapsed += dt * Animations.speed();
        if (elapsed >= delaySeconds + durationSeconds) running = false;
    }

    public float progress() {
        if (!running) return 1f;
        float t = (elapsed - delaySeconds) / durationSeconds;
        return t <= 0f ? 0f : Math.min(1f, t);
    }

    public float eased() {
        return running ? easing.apply(progress()) : 1f;
    }

    public float value() {
        return running ? Interpolate.lerp(from, to, eased()) : to;
    }

    public int colorValue(int fromColor, int toColor) {
        return running ? Interpolate.color(fromColor, toColor, eased()) : toColor;
    }

    public boolean running() {
        return running;
    }

    public float target() {
        return to;
    }

    public void snap(float value) {
        from = value;
        to = value;
        running = false;
    }
}
