package dev.henny.cosmetico.anim;

public final class CatmullRom {
    private CatmullRom() {}

    public static float at(float t, float p0, float p1, float p2, float p3) {
        float v0 = (p2 - p0) * 0.5f;
        float v1 = (p3 - p1) * 0.5f;
        float t2 = t * t;
        float t3 = t * t2;
        return (2f * p1 - 2f * p2 + v0 + v1) * t3 + (-3f * p1 + 3f * p2 - 2f * v0 - v1) * t2 + v0 * t + p1;
    }
}
