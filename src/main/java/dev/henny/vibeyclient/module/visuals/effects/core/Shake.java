package dev.henny.vibeyclient.module.visuals.effects.core;

public final class Shake {
    public static final float DECAY = 3.2f;
    public static final float ROLL_DEG = 3.5f;
    public static final float PITCH_DEG = 1.8f;
    public static final float MAX_DT = 0.1f;
    public static final float REACH = 4f;
    public static final float MIN_REACH = 8f;

    private float energy;
    private float time;

    public void kick(float power) {
        energy = Math.min(1f, Math.max(energy, power));
    }

    public void step(float dt) {
        float t = Math.max(0f, Math.min(MAX_DT, dt));
        time += t;
        energy *= (float) Math.exp(-DECAY * t);
        if (energy < 0.002f) energy = 0;
    }

    public float energy() {
        return energy;
    }

    public float roll(float strength) {
        return amplitude(strength) * ROLL_DEG * wave(time, 23f, 37f, 0.3f);
    }

    public float pitch(float strength) {
        return amplitude(strength) * PITCH_DEG * wave(time, 29f, 17f, 1.7f);
    }

    public static float power(double distance, float radius) {
        float reach = Math.max(MIN_REACH, radius * REACH);
        return (float) Math.max(0, Math.min(1, 1 - distance / reach));
    }

    private float amplitude(float strength) {
        return energy * energy * Math.max(0f, Math.min(1f, strength));
    }

    private static float wave(float t, float f1, float f2, float phase) {
        return (float) (0.7 * Math.sin(t * f1 + phase) + 0.3 * Math.sin(t * f2 + phase * 2));
    }
}
