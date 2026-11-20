package dev.henny.vui.text.effect;

import dev.henny.vui.text.GlyphState;
import dev.henny.vui.text.TextEffect;

public record Shake(float amplitude, float speed) implements TextEffect {

    @Override
    public void apply(GlyphState g) {
        int tick = (int) (g.time * speed);
        g.dx += noise(g.index * 31 + tick) * amplitude;
        g.dy += noise(g.index * 17 + tick * 7 + 3) * amplitude;
    }

    private static float noise(int seed) {
        int x = seed * 0x27d4eb2d;
        x ^= x >>> 15;
        x *= 0x85ebca6b;
        x ^= x >>> 13;
        return ((x & 0xFFFF) / 32767.5f) - 1f;
    }
}
