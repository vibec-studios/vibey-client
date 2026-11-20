package dev.henny.cosmetico.sim;

public final class Approach {
    private Approach() {}

    public static float to(float v, float target, float k) {
        return v + (target - v) * k;
    }
}
