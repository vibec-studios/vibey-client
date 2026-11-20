package dev.henny.cosmetico.texture;

import dev.henny.cosmetico.model.Texture;

public record StripFrame(int a, int b, int q) {
    public static StripFrame at(Texture t, long ticks, float partial) {
        long cycle = t.cycleTicks();
        long pos = ticks % cycle;
        int step = (int) (pos / t.frameTime);
        int a = t.frames[step % t.frames.length];
        int b = t.frames[(step + 1) % t.frames.length];
        float mix = t.interpolate ? ((pos % t.frameTime) + partial) / t.frameTime : 0f;
        int q = t.interpolate ? Math.min(7, (int) (mix * 8f)) : 0;
        return new StripFrame(a, b, q);
    }

    public int key() {
        return a * 4096 + b * 8 + q;
    }

    public float blend() {
        return q / 8f;
    }
}
