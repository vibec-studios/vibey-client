package dev.henny.cosmetico.texture;

public final class StripPixels {
    private StripPixels() {}

    public static int lerp(int a, int b, float t) {
        int out = 0;
        for (int s = 0; s < 32; s += 8) {
            int ca = (a >>> s) & 0xFF, cb = (b >>> s) & 0xFF;
            out |= (Math.round(ca + (cb - ca) * t) & 0xFF) << s;
        }
        return out;
    }
}
